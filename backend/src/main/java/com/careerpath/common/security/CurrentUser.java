package com.careerpath.common.security;

import java.util.UUID;

/**
 * 当前用户上下文。
 *
 * <p>MVP 尚未接入认证，统一使用演示用户。接入认证后由安全上下文替换，
 * 调用方只依赖本类暴露的常量与方法签名。
 */
public final class CurrentUser {

    public static final UUID DEMO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private CurrentUser() {
    }

    public static UUID id() {
        return DEMO_USER_ID;
    }
}