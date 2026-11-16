package com.health.healthserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.health.healthserver.entity.Admin;
import com.health.healthserver.response.R;
import com.health.healthserver.service.AdminService;
import com.health.healthserver.utils.JwtUtil;
import com.health.healthserver.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name="管理者")
@RestController
public class AdminController {
    @Resource
    private AdminService adminService;
    @Operation(summary = "管理员新增")
    @PostMapping("/admin/add")
    public R<?> add(@RequestBody Admin admin){
        adminService.save(admin);
        return R.success();
    }
    @Operation(summary = "管理员分页模糊查询列表")
    @PostMapping("/admin/list")
    public R<PageInfo<Admin>>list(@RequestBody Admin admin,@RequestBody Integer pageNumber,@RequestBody Integer pageSize){
        LambdaQueryWrapper<Admin>lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.like(admin.getUsername()!=null,Admin::getUsername,admin.getUsername());
        lambdaQueryWrapper.like(admin.getPhone()!=null,Admin::getPhone,admin.getPhone());
        PageHelper.startPage(pageNumber,pageSize);
        List<Admin>list=adminService.list(lambdaQueryWrapper);
        PageInfo<Admin>pageInfo=new PageInfo<>(list);
        return R.data(pageInfo);
    }
    @Operation(summary = "管理员修改")
    @PostMapping("/admin/update")
    public R<?> update(@RequestBody Admin admin){
        adminService.updateById(admin);
        return R.success();
    }
    @Operation(summary = "管理员删除")
    @PostMapping("/admin/del")
    public R<?> delete(@RequestParam Long id){
        adminService.removeById(id);
        return R.success();
    }
    @Resource
    private JwtUtil jwtUtil;
    @PostMapping("/admin/login")
    public R<?> login(@RequestBody Admin loginForm) {
        // 1. 根据用户名查询用户
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, loginForm.getUsername());
        Admin user = adminService.getOne(wrapper);

        // 2. 判断用户是否存在
        if (user == null) {
            return R.fail("用户不存在");
        }

        // 3. 校验密码（注意：这里比对的是前端传来的 password 和数据库查出来的 password）
        // 因为你在实体里已经把 userpwd 改名为 password 了，所以直接 getPassword() 即可
        if (!user.getPassword().equals(loginForm.getPassword())) {
            return R.fail("密码错误");
        }
        // 4. 生成假 Token（生产环境要用 JWT）
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 5. 返回用户信息和 Token
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", user);

        return R.data(data);
    }
    // 获取当前登录用户信息
    @GetMapping("/admin/info")
    public R<Admin> info() {
        Long userId = UserContext.getUserId();
        if (userId == null) return R.fail("未登录");
        Admin user = adminService.getById(userId);
        if (user != null) user.setPassword(null); // 不返回密码
        return R.data(user);
    }

    // 修改基本信息
    @PostMapping("/admin/update-info")
    public R<?> updateInfo(@RequestBody Admin form) {
        Long userId = UserContext.getUserId();
        if (userId == null) return R.fail("未登录");

        Admin update = new Admin();
        update.setId(userId);
        update.setNickname(form.getNickname());
        update.setEmail(form.getEmail());
        update.setPhone(form.getPhone());
        update.setAvatar(form.getAvatar());
        adminService.updateById(update);

        return R.success("修改成功");
    }

    // 修改密码
    @PostMapping("/admin/update-pwd")
    public R<?> updatePwd(@RequestBody Map<String, String> form) {
        Long userId = UserContext.getUserId();
        if (userId == null) return R.fail("未登录");

        Admin user = adminService.getById(userId);
        if (user == null) return R.fail("用户不存在");

        if (!user.getPassword().equals(form.get("oldPassword"))) {
            return R.fail("原密码错误");
        }
        if (form.get("newPassword").length() < 6) {
            return R.fail("新密码至少 6 位");
        }

        Admin update = new Admin();
        update.setId(userId);
        update.setPassword(form.get("newPassword"));  // 生产环境记得加密！
        adminService.updateById(update);

        return R.success("密码修改成功");
    }
    @PostMapping("/admin/upload-avatar")
    public R<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getUserId();
        if (userId == null) return R.fail("未登录");
        if (file.isEmpty()) return R.fail("文件为空");

        // 生成文件名
        String originalName = file.getOriginalFilename();
        String ext = originalName.substring(originalName.lastIndexOf("."));
        String fileName = "avatar_" + userId + "_" + System.currentTimeMillis() + ext;

        // 保存路径：项目根目录/upload/avatar/
        String uploadDir = System.getProperty("user.dir") + "/upload/avatar/";
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        try {
            file.transferTo(new File(uploadDir + fileName));
        } catch (IOException e) {
            return R.fail("上传失败: " + e.getMessage());
        }

        // 返回可访问的 URL（需要配置静态资源映射）
        String url = "/admin/upload/avatar/" + fileName;
        return R.data(url);
    }
}
