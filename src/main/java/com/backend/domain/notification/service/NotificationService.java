package com.backend.domain.notification.service;

import com.backend.domain.notification.dto.NotificationListResponseDto;
import com.backend.domain.notification.repository.NotificationRepository;
import com.backend.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;


}
