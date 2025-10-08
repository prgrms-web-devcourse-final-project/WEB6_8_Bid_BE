package com.backend.global.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @DistributedLock 선언 시 수행되는 Aop class
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAop {
    
    private static final String REDISSON_LOCK_PREFIX = "LOCK:";
    
    private final RedissonClient redissonClient;
    private final AopForTransaction aopForTransaction;
    
    @Around("@annotation(com.backend.global.lock.DistributedLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);
        
        String key = REDISSON_LOCK_PREFIX + CustomSpringELParser.getDynamicValue(
                signature.getParameterNames(),
                joinPoint.getArgs(),
                distributedLock.key()
        );
        
        RLock rLock = redissonClient.getLock(key); // (1)
        
        try {
            boolean available = rLock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            ); // (2)
            
            if (!available) {
                log.warn("Redisson Lock 획득 실패 - method: {}, key: {}", method.getName(), key);
                return false;
            }
            
            log.debug("Redisson Lock 획득 성공 - method: {}, key: {}", method.getName(), key);
            return aopForTransaction.proceed(joinPoint); // (3)
            
        } catch (InterruptedException e) {
            log.error("Redisson Lock 인터럽트 발생 - method: {}, key: {}", method.getName(), key);
            throw new InterruptedException();
        } finally {
            try {
                if (rLock.isHeldByCurrentThread()) {
                    rLock.unlock(); // (4)
                    log.debug("Redisson Lock 해제 - method: {}, key: {}", method.getName(), key);
                }
            } catch (IllegalMonitorStateException e) {
                log.info("Redisson Lock Already UnLock - serviceName: {}, key: {}", method.getName(), key);
            }
        }
    }
}
