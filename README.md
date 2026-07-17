# 智能客服助手系统 (AI Customer Service)

基于 **LangChain4j + DeepSeek + Spring Boot 3** 的智能客服助手系统。用户通过自然语言对话即可查询订单、统计消费、查看商品销量，支持流式输出（打字机效果）。

## 功能特性

- **自然语言对话**：用户用中文提问，AI 自动理解意图并回答
- **Function Calling**：AI 自主调用 6 个工具方法（订单查询、消费统计、销量排行等）
- **流式输出**：基于 SSE (Server-Sent Events) 逐 token 推送，打字机效果
- **会话记忆**：基于 ChatMemory 保留最近 10 轮对话上下文
- **订单管理**：完整的 CRUD REST API
- **API 文档**：Swagger UI 自动生成接口文档
- **可观测性**：Micrometer 指标采集 + OpenTelemetry 链路追踪
- **一键部署**：Docker Compose 编排 MySQL + 应用

## 技术栈

| 层次 | 技术 | 说明 |
|------|------|------|
| 语言 | Java 17 | LTS 版本 |
| 框架 | Spring Boot 3.4 | 自动配置、IoC/AOP |
| Web | Spring MVC + WebFlux | 同步接口 + 响应式流式接口 |
| AI 框架 | LangChain4j 1.17 | AiService、Function Calling、ChatMemory |
| LLM | DeepSeek | OpenAI 兼容 API |
| ORM | Spring Data JPA | Repository 模式，方法命名查询 |
| 数据库 | MySQL 8.0 | InnoDB 引擎 |
| 参数校验 | Hibernate Validator | JSR-380 规范 |
| API 文档 | springdoc-openapi | OpenAPI 3 规范 |
| 可观测性 | Micrometer + OpenTelemetry | 指标 + 链路追踪 (Zipkin) |
| 构建 | Maven | 依赖管理、打包 |
| 部署 | Docker + Docker Compose | 容器化、多服务编排 |

## 项目结构

```
src/main/java/com/ai/assistant/
├── Application.java              # 启动类
├── config/
│   ├── AssistantConfiguration.java  # AI 服务配置（ChatMemory、Listener）
│   └── OpenApiConfig.java          # Swagger 文档配置
├── controller/
│   ├── AssistantController.java    # AI 对话接口（同步+流式）
│   ├── ChatModelController.java    # 低层 ChatModel 接口
│   └── OrderController.java        # 订单 CRUD 接口
├── service/
│   ├── Assistant.java              # 同步 AI 服务接口
│   ├── StreamingAssistant.java     # 流式 AI 服务接口
│   └── AssistantTools.java         # AI 工具集（Function Calling）
├── model/
│   └── Order.java                  # 订单实体类
├── repository/
│   └── OrderRepository.java        # 订单数据访问层
├── dto/
│   ├── ChatRequest.java            # 聊天请求 DTO
│   └── OrderRequest.java           # 订单创建 DTO
├── common/
│   ├── Result.java                 # 统一返回格式
│   ├── BusinessException.java      # 自定义业务异常
│   └── GlobalExceptionHandler.java # 全局异常处理器
└── listener/
    └── ChatModelListenerImpl.java  # LLM 请求/响应监听器
```

## 快速启动

### 方式一：Docker Compose 一键启动（推荐）

```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env，填入你的 DeepSeek API Key

# 2. 一键启动
docker-compose up -d

# 3. 访问应用
# 聊天页面: http://localhost:8082
# Swagger 文档: http://localhost:8082/swagger-ui.html
```

### 方式二：本地启动

```bash
# 前置条件：本地已安装 MySQL 8.x 和 Java 17

# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE agent;"

# 2. 配置环境变量
export DEEPSEEK_API_KEY=your_api_key_here

# 3. 编译启动
./mvnw spring-boot:run
```

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/chat?message=你好` | 同步对话 |
| POST | `/api/chat` | 同步对话（JSON Body + 参数校验） |
| GET | `/api/chat/stream?message=你好` | 流式对话（SSE） |
| GET | `/model?message=Hello` | 直接调用 ChatModel（无记忆） |
| GET | `/api/orders` | 查询所有订单 |
| GET | `/api/orders/{id}` | 按 ID 查询订单 |
| GET | `/api/orders/user/{username}` | 按用户名查询订单 |
| POST | `/api/orders` | 创建订单 |
| DELETE | `/api/orders/{id}` | 删除订单 |

## 使用示例

### 同步对话
```
GET /api/chat?message=张三买了什么东西？
```

### 流式对话
```
GET /api/chat/stream?message=哪个商品卖得最好？
```

### 创建订单
```
POST /api/orders
Content-Type: application/json

{
  "username": "张三",
  "product": "iPhone 15",
  "amount": 5999.00
}
```
