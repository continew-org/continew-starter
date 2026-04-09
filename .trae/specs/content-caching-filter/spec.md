# CommonContentCachingFilter 优化 - Product Requirement Document

## Overview
- **Summary**: 优化 continew-starter-core 中的 CommonContentCachingFilter，解决重复包装问题，支持文件上传（保留文件部分，同时缓存其他参数）和流式响应场景，尽量复用 Spring 官方的 ContentCachingRequestWrapper 和 ContentCachingResponseWrapper 以获得更好的兼容性。
- **Purpose**: 为各个 starter 提供统一、可靠的请求/响应内容缓存机制，解决在 controller 处理前或处理后拦截获取请求参数、响应参数时遇到的问题，特别是文件上传和流式响应场景。
- **Target Users**: continew-starter 的使用者，需要在 filter 或 interceptor 中读取请求/响应内容的开发者。

## Goals
- 优化 CommonContentCachingFilter，解决请求/响应包装器重复包装问题
- 支持文件上传场景：文件部分直接透传，其他参数仍然可以缓存读取
- 支持流式响应场景（如 SSE）：不缓存流式内容，直接透传
- 尽量复用 Spring 官方的 ContentCachingRequestWrapper 和 ContentCachingResponseWrapper
- 提供配置化能力，允许用户灵活配置

## Non-Goals (Out of Scope)
- 不修改 Spring 官方的 ContentCachingRequestWrapper 和 ContentCachingResponseWrapper 源码
- 不实现复杂的流式内容缓存机制
- 不处理超大文件上传的内存溢出问题（依赖配置的 cacheLimit）

## Background & Context
- 当前项目中已经存在 CommonContentCachingFilter、RepeatReadRequestWrapper、RepeatReadResponseWrapper、RepeatableContentCachingRequestWrapper 等实现
- Spring 官方的 ContentCachingRequestWrapper 和 ContentCachingResponseWrapper 存在一些限制：
  - ContentCachingRequestWrapper 不提前消费请求体，需要在应用消费后才能通过 getContentAsByteArray() 获取
  - 在文件上传（multipart/form-data）和流式响应（如 SSE）场景下不能直接使用
- Spring 官方 issue #24533 和 #25046 讨论了相关问题，官方建议通过继承 ContentCachingRequestWrapper 来实现可重复读取

## Functional Requirements
- **FR-1**: CommonContentCachingFilter 能够检测并避免重复包装请求和响应
- **FR-2**: 支持文件上传场景：文件部分直接透传，其他表单参数可以缓存读取
- **FR-3**: 支持流式响应场景：检测并透传流式响应，不进行缓存
- **FR-4**: 复用 Spring 官方的 ContentCachingRequestWrapper 和 ContentCachingResponseWrapper 作为基础
- **FR-5**: 保持现有的配置能力（cacheLimit、excludePatterns）

## Non-Functional Requirements
- **NFR-1**: 性能影响最小化，缓存操作不应显著增加请求处理时间
- **NFR-2**: 兼容性良好，与 Spring 生态系统的其他组件正常协作
- **NFR-3**: 代码清晰易读，遵循项目现有代码风格

## Constraints
- **Technical**: 基于 Spring Framework 6.x，使用 Jakarta EE 9+ API
- **Business**: 保持向后兼容，不破坏现有功能
- **Dependencies**: 依赖 Spring Web 模块

## Assumptions
- 用户会合理配置 cacheLimit 以避免内存溢出
- 文件上传场景中，非文件参数仍然可以被正常解析和缓存
- 流式响应可以通过 Content-Type 或其他特征准确识别

## Acceptance Criteria

### AC-1: 避免重复包装
- **Given**: 请求/响应已经被包装过
- **When**: CommonContentCachingFilter 执行
- **Then**: 不会再次包装，直接使用已有的包装器
- **Verification**: `programmatic`

### AC-2: 文件上传场景支持
- **Given**: 一个 multipart/form-data 请求，包含文件和其他表单参数
- **When**: 请求通过 CommonContentCachingFilter
- **Then**: 文件部分直接透传，其他表单参数可以被缓存和读取
- **Verification**: `programmatic`

### AC-3: 流式响应场景支持
- **Given**: 一个流式响应（如 SSE，Content-Type 为 text/event-stream）
- **When**: 响应通过 CommonContentCachingFilter
- **Then**: 响应内容直接透传，不进行缓存
- **Verification**: `programmatic`

### AC-4: 复用 Spring 官方类
- **Given**: CommonContentCachingFilter 实现
- **When**: 检查代码实现
- **Then**: 请求包装器继承自 ContentCachingRequestWrapper，响应包装器继承自 ContentCachingResponseWrapper
- **Verification**: `human-judgment`

### AC-5: 配置能力保持
- **Given**: ContentCachingProperties 配置
- **When**: 应用不同的配置值
- **Then**: cacheLimit 和 excludePatterns 配置正常生效
- **Verification**: `programmatic`

## Open Questions
- [ ] 是否需要支持更多的流式响应类型识别方式？
- [ ] 文件上传场景中，是否需要提供选项来完全禁用缓存？
