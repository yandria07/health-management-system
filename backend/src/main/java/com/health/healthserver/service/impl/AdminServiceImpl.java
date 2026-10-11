package com.health.healthserver.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.healthserver.entity.Admin;
import com.health.healthserver.mapper.AdminMapper;
import com.health.healthserver.service.AdminService;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin>implements AdminService {
}
