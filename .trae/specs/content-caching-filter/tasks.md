# CommonContentCachingFilter 优化 - The Implementation Plan (Decomposed and Prioritized Task List)

## [x] Task 1: 分析和整理现有代码
- **Priority**: P0
- **Depends On**: None
- **Description**: 
  - 详细分析现有的 CommonContentCachingFilter、RepeatReadRequestWrapper、RepeatReadResponseWrapper、RepeatableContentCachingRequestWrapper
  - 识别需要保留、重构或废弃的代码
  - 理解现有实现的优缺点
- **Acceptance Criteria Addressed**: [AC-4]
- **Test Requirements**:
  - `human-judgement` TR-1.1: 代码分析报告清晰，识别出现有实现的关键问题
- **Notes**: 重点关注重复包装检测、文件上传处理、流式响应处理

## [x] Task 2: 优化 RepeatableContentCachingRequestWrapper 支持文件上传
- **Priority**: P0
- **Depends On**: [Task 1]
- **Description**: 
  - 基于 Spring 官方的 ContentCachingRequestWrapper，优化 RepeatableContentCachingRequestWrapper
  - 添加对 multipart/form-data 请求的支持：文件部分直接透传，其他参数缓存
  - 保持可重复读取的能力
- **Acceptance Criteria Addressed**: [AC-2, AC-4]
- **Test Requirements**:
  - `programmatic` TR-2.1: 对于 multipart/form-data 请求，文件可以正常上传
  - `programmatic` TR-2.2: 对于 multipart/form-data 请求，非文件参数可以被缓存和读取
  - `programmatic` TR-2.3: 请求体可以被多次读取
- **Notes**: 参考 RepeatReadRequestWrapper 的实现思路

## [x] Task 3: 优化响应包装器支持流式响应
- **Priority**: P0
- **Depends On**: [Task 1]
- **Description**: 
  - 创建一个继承自 ContentCachingResponseWrapper 的响应包装器
  - 添加对流式响应（如 SSE）的检测和透传支持
  - 保持 copyBodyToResponse() 方法的兼容性
- **Acceptance Criteria Addressed**: [AC-3, AC-4]
- **Test Requirements**:
  - `programmatic` TR-3.1: 对于 text/event-stream 响应，内容直接透传不缓存
  - `programmatic` TR-3.2: 对于普通响应，内容正常缓存并可以读取
  - `programmatic` TR-3.3: copyBodyToResponse() 方法正常工作
- **Notes**: 参考 RepeatReadResponseWrapper 的实现思路

## [x] Task 4: 优化 CommonContentCachingFilter
- **Priority**: P0
- **Depends On**: [Task 2, Task 3]
- **Description**: 
  - 优化 CommonContentCachingFilter，使用新的请求/响应包装器
  - 改进重复包装检测逻辑
  - 移除对 multipart 请求的完全排除，改为智能处理
- **Acceptance Criteria Addressed**: [AC-1, AC-2, AC-3]
- **Test Requirements**:
  - `programmatic` TR-4.1: 请求和响应不会被重复包装
  - `programmatic` TR-4.2: 文件上传请求正常处理
  - `programmatic` TR-4.3: 流式响应正常处理
  - `programmatic` TR-4.4: 普通请求正常缓存和读取
- **Notes**: 保持与 ContentCachingProperties 的兼容性

## [x] Task 5: 保持配置能力并测试
- **Priority**: P1
- **Depends On**: [Task 4]
- **Description**: 
  - 确保 ContentCachingProperties 的 cacheLimit 和 excludePatterns 配置正常工作
  - 编写单元测试和集成测试
  - 进行各种场景的手动测试
- **Acceptance Criteria Addressed**: [AC-5]
- **Test Requirements**:
  - `programmatic` TR-5.1: cacheLimit 配置生效
  - `programmatic` TR-5.2: excludePatterns 配置生效
  - `human-judgement` TR-5.3: 测试覆盖所有主要场景
- **Notes**: 测试场景包括：普通请求、文件上传、流式响应、排除路径等

## [x] Task 6: 代码清理和文档更新
- **Priority**: P2
- **Depends On**: [Task 5]
- **Description**: 
  - 清理不再需要的旧代码（如 RepeatReadRequestWrapper、RepeatReadResponseWrapper）
  - 更新相关的 JavaDoc
  - 确保代码风格一致
- **Acceptance Criteria Addressed**: [AC-4]
- **Test Requirements**:
  - `human-judgement` TR-6.1: 代码清理后没有编译错误
  - `human-judgement` TR-6.2: JavaDoc 完整准确
- **Notes**: 考虑向后兼容性，谨慎删除代码
