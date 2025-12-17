# Expensight - Receipt Upload System

Expensight is a receipt upload and expense intelligence system that combines OCR and LLM-based understanding to extract structured financial data from receipt images. Built with Spring Boot following enterprise-grade best practices, it provides a complete end-to-end solution for receipt management.

## Features

- **OAuth2 Authentication** - Secure login with Google (extensible to GitHub, Microsoft)
- **File Upload** - Supports JPG, PNG, JPEG, and PDF files (max 10MB)
- **OCR Integration** - Tesseract OCR with extensible architecture for multiple providers
- **Intelligent Data Extraction** - LLM-powered extraction of:
  - Merchant name
  - Total amount
  - Receipt date
  - Tax amount (if available)
  - Currency detection (INR, USD, EUR, GBP, JPY, etc.)
  - Items with quantity and price
- **Data Persistence** - H2 database with JPA/Hibernate (easily switchable to PostgreSQL/MySQL)
- **Dashboard UI** - Clean table layout displaying all receipt entries with upload functionality

## Architecture Overview

### Design Principles

The system follows **SOLID principles** and implements several **design patterns**:

- **Strategy Pattern**: OCR and LLM services are interchangeable (Tesseract/Google Vision, OpenRouter/other LLMs)
- **Factory Pattern**: Service factories for selecting appropriate providers
- **Service Layer Pattern**: Business logic separated from controllers
- **Repository Pattern**: Data access abstraction via Spring Data JPA
- **DTO Pattern**: Clean API responses with `ReceiptResponse` DTOs
- **Exception Hierarchy**: Centralized exception handling with `@ControllerAdvice`

### Architecture Layers

```
┌─────────────────────────────────────┐
│         Presentation Layer          │
│  (Thymeleaf Templates + REST API)   │
│  + GlobalExceptionHandler            │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│         Controller Layer             │
│  (HomeController, ReceiptController) │
│  - Thin controllers, no exception    │
│    handling (delegated to handler)   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│         Service Layer                │
│  (ReceiptService, OcrService,       │
│   LlmService, ReceiptParserService)  │
│  - Business logic                    │
│  - Domain-specific exceptions        │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│         Repository Layer             │
│  (ReceiptRepository)                 │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│         Database (H2)                │
└─────────────────────────────────────┘
```

### Exception Handling Architecture

The system implements a **centralized exception handling** pattern:

```
exception/
├── BaseException (abstract root)
├── ResourceNotFoundException
├── ValidationException
├── OcrException
├── LlmException
├── FileStorageException
├── UnauthorizedException
└── ForbiddenException

web/exception/
└── GlobalExceptionHandler (@ControllerAdvice)
    - Handles all exceptions
    - Returns consistent ErrorResponse DTOs
    - Maps to appropriate HTTP status codes
```

**Benefits:**
- Clean controllers (no try-catch blocks)
- Consistent error responses
- Proper HTTP status code mapping
- Easy to extend with new exception types

### Processing Flow

1. **Upload**: User uploads receipt image/PDF
2. **Validation**: File type and size validation
3. **Storage**: File stored locally with metadata
4. **OCR**: Tesseract extracts raw text from image/PDF
5. **Normalization**: Currency symbols normalized (₹ correction for Indian receipts)
6. **LLM Parsing**: GPT-4o-mini extracts structured data via OpenRouter
7. **Persistence**: Receipt and items saved to database
8. **Display**: Dashboard shows all receipts in table format with proper currency symbols

## Data Model

### Receipt Entity
```java
- id: UUID (Primary Key)
- userEmail: String (User identification)
- merchantName: String
- totalAmount: BigDecimal
- receiptDate: LocalDate
- taxAmount: BigDecimal (nullable)
- currency: String (default: INR, supports USD, EUR, etc.)
- rawOcrText: String (TEXT, for reference/reprocessing)
- fileMetadata: FileMetadata (embedded)
- status: ProcessingStatus (PENDING, PROCESSING, COMPLETED, FAILED)
- failureReason: String (nullable)
- items: List<ReceiptItem>
- createdAt: LocalDateTime
```

### ReceiptItem Entity
```java
- id: UUID (Primary Key)
- receipt: Receipt (ManyToOne relationship, @JsonIgnore to prevent circular refs)
- itemName: String
- quantity: Integer (default: 1)
- price: BigDecimal
- total: BigDecimal (calculated: price × quantity)
```

### FileMetadata (Embedded)
```java
- id: UUID
- fileName: String
- contentType: String
- storagePath: String
- uploadedAt: LocalDateTime
```

**Note**: Receipts use `userEmail` (extracted from OAuth2 authentication) for user identification. User information is obtained directly from OAuth2User attributes without persisting User entities.

## Setup & Run Instructions

### Prerequisites

- **Java 21** or higher
- **Maven 3.6+**
- **Tesseract OCR** installed:
  - **macOS**: `brew install tesseract`
  - **Linux**: `sudo apt-get install tesseract-ocr`
  - **Windows**: Download from [GitHub](https://github.com/UB-Mannheim/tesseract/wiki)

### Configuration

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd expensight
   ```

2. **Set up OAuth2 credentials**
   
   Create a Google OAuth2 application:
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create a new project or select existing
   - Enable Google+ API
   - Create OAuth 2.0 credentials
   - Add authorized redirect URI

3. **Configure environment variables**
   
   Create `.env` file or set environment variables:
   ```bash
   export GOOGLE_CLIENT_ID="your-client-id"
   export GOOGLE_CLIENT_SECRET="your-client-secret"
   export OPENROUTER_API_KEY="your-openrouter-api-key"  # Required for LLM parsing
   export TESSERACT_DATA_PATH="/opt/homebrew/share/tessdata"  # macOS default
   ```

4. **Update application.properties** (if not using env vars)
   ```properties
   spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
   spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
   llm.openrouter.api-key=${OPENROUTER_API_KEY}
   ocr.tesseract.data-path=${TESSERACT_DATA_PATH:/opt/homebrew/share/tessdata}
   ```

### Running the Application

1. **Build the project**
   ```bash
   mvn clean install
   ```

2. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

3. **Access the application**
   - Open browser: `http://localhost:8080`
   - Click "Sign in with Google"
   - Upload receipts from the dashboard
   - View receipts in table format with proper currency symbols

### Running Tests

```bash
mvn test
```

**Test Coverage:**
- Unit tests for services (OCR, LLM, Receipt processing)
- Integration tests for controllers
- TDD approach followed throughout

## Technology Stack

### OCR
- **Tesseract OCR** (v5.8.0 via Tess4J)
  - Primary OCR engine
  - Supports images (JPG, PNG) and PDFs
  - Automatic library path detection via `TesseractLibraryInitializer`
  - Currency symbol normalization for Indian receipts (₹ correction)
  - Extensible architecture for Google Vision API and similar OCR providers

### LLM
- **OpenRouter API** with **GPT-4o-mini** (default: `openai/gpt-4o-mini`)
  - Structured data extraction from OCR text
  - JSON response parsing with validation
  - Temperature: 0.2 for consistent parsing
  - Extensible to other models (Ollama, Claude, Gemini, etc.)

### Core Technologies
- **Framework**: Spring Boot 3.4.12
- **Language**: Java 21
- **Database**: H2 (in-memory, can be switched to PostgreSQL/MySQL)
- **ORM**: Spring Data JPA / Hibernate
- **Security**: Spring Security with OAuth2
- **Templating**: Thymeleaf
- **Build Tool**: Maven
- **Testing**: JUnit 5, Mockito, Spring Boot Test
- **HTTP Client**: WebClient (reactive) for LLM API calls
- **PDF Processing**: Apache PDFBox 3.0.1

## Architecture Design

### LLM-Based Data Extraction

The system uses an **LLM-based agent** (GPT-4o-mini via OpenRouter) to intelligently extract structured data from OCR text. This design choice provides:

1. **Accuracy**: LLMs excel at understanding context and extracting structured information from unstructured text, even when OCR quality is imperfect
2. **Flexibility**: The agent can handle various receipt formats without hardcoded parsing rules
3. **Extensibility**: Easy to switch LLM providers or models (OpenRouter supports multiple providers)
4. **Prompt Engineering**: Carefully crafted prompts ensure consistent JSON output with all required fields
5. **Error Handling**: The agent gracefully handles missing or ambiguous data, marking fields as optional

**Agent Workflow:**
1. OCR extracts raw text from receipt image/PDF
2. Text is normalized (currency symbols, formatting)
3. LLM agent receives prompt with OCR text and extraction instructions
4. Agent returns structured JSON with all receipt fields
5. System validates and persists extracted data

### Exception Handling

- **Centralized Exception Handler**: `@ControllerAdvice` pattern for consistent error responses
- **Exception Hierarchy**: Base exception with domain-specific exceptions
- **Error Response DTOs**: Structured error responses with error codes
- **Benefits**: Clean controllers, consistent API, easy debugging

### Extensibility

- OCR and LLM services implement interfaces, allowing easy addition of new providers
- Factory pattern enables runtime provider selection
- Configuration-driven provider switching
- OAuth2 implementation is provider-agnostic

### Separation of Concerns

- **Controllers**: Handle HTTP requests/responses only, delegate to services
- **Services**: Business logic and orchestration
- **Repositories**: Data access abstraction
- **DTOs**: Clean API contracts, prevent circular references
- **Exception Handler**: Centralized error handling

### Error Handling & Resilience

- Graceful degradation: Receipts saved even if OCR/LLM fails
- Status tracking: PENDING → PROCESSING → COMPLETED/FAILED
- Detailed error messages for debugging
- Global exception handler provides consistent error responses

### Testability

- Interfaces enable easy mocking
- Unit tests for all services
- Integration tests for controllers
- High test coverage maintained
- Test configuration with mock properties

### Multi-Currency Support

- Currency field in Receipt entity
- Dynamic currency symbol display in UI (₹, $, €, £, ¥)
- Currency-aware formatting in dashboard
- Automatic currency detection from receipt content

### Performance Considerations

- Lazy loading for relationships
- Efficient file storage with metadata
- Async processing capability (future enhancement)
- Optimistic locking for concurrent updates

## API Endpoints

### REST API

- `POST /receipts/upload` - Upload receipt file
  - **Request**: Multipart file
  - **Response**: `UploadReceiptResponse` with receiptId, fileName, status
  - **Errors**: 400 (Validation), 401 (Unauthorized), 500 (Server Error)

- `GET /receipts` - Get all receipts for authenticated user
  - **Response**: `List<ReceiptResponse>` with merchant, date, items, tax, total
  - **Errors**: 401 (Unauthorized)

- `GET /receipts/{receiptId}` - Get specific receipt details
  - **Response**: `ReceiptResponse`
  - **Errors**: 401 (Unauthorized), 403 (Forbidden), 404 (Not Found)

**Error Response Format:**
```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "File cannot be null or empty",
  "timestamp": "2025-12-16T18:00:00",
  "path": "/receipts/upload",
  "details": []
}
```

### Web Pages

- `GET /` - Landing page with OAuth login
- `GET /home` - Dashboard with upload and receipt list (table format)
- `GET /logout` - Logout (redirects to home)

## Project Structure

```
expensight/
├── src/
│   ├── main/
│   │   ├── java/com/gm/expensight/
│   │   │   ├── config/              # Configuration classes
│   │   │   │   ├── JpaConfig.java
│   │   │   │   ├── OcrConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── TesseractLibraryInitializer.java
│   │   │   │   └── WebClientConfig.java
│   │   │   ├── domain/model/        # JPA entities
│   │   │   │   ├── Receipt.java
│   │   │   │   ├── ReceiptItem.java
│   │   │   │   ├── FileMetadata.java
│   │   │   │   └── ProcessingStatus.java
│   │   │   ├── exception/           # Exception hierarchy
│   │   │   │   ├── BaseException.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── ValidationException.java
│   │   │   │   ├── OcrException.java
│   │   │   │   ├── LlmException.java
│   │   │   │   ├── FileStorageException.java
│   │   │   │   ├── UnauthorizedException.java
│   │   │   │   └── ForbiddenException.java
│   │   │   ├── repository/          # Data access layer
│   │   │   │   ├── ReceiptRepository.java
│   │   │   │   └── ReceiptItemRepository.java
│   │   │   ├── security/            # OAuth2 configuration
│   │   │   │   ├── CustomOAuth2UserService.java
│   │   │   │   └── CustomOAuth2AuthorizationRequestResolver.java
│   │   │   ├── service/             # Business logic
│   │   │   │   ├── dto/              # Service DTOs
│   │   │   │   │   └── ReceiptParsingResult.java
│   │   │   │   ├── impl/             # Service implementations
│   │   │   │   │   ├── ReceiptServiceImpl.java
│   │   │   │   │   ├── TesseractOcrService.java
│   │   │   │   │   ├── GoogleVisionOcrService.java
│   │   │   │   │   ├── OpenRouterLlmService.java
│   │   │   │   │   ├── ReceiptParserServiceImpl.java
│   │   │   │   │   ├── PromptServiceImpl.java
│   │   │   │   │   └── LocalFileStorageService.java
│   │   │   │   ├── util/             # Utility classes
│   │   │   │   │   ├── PdfToImageConverter.java
│   │   │   │   │   └── OcrTextNormalizer.java
│   │   │   │   ├── OcrService.java
│   │   │   │   ├── OcrServiceFactory.java
│   │   │   │   ├── LlmService.java
│   │   │   │   ├── LlmServiceFactory.java
│   │   │   │   ├── ReceiptService.java
│   │   │   │   ├── ReceiptParserService.java
│   │   │   │   ├── PromptService.java
│   │   │   │   ├── FileStorageService.java
│   │   │   │   ├── FileValidator.java
│   │   │   │   └── ReceiptMapper.java
│   │   │   └── web/                  # Controllers and DTOs
│   │   │       ├── dto/              # API DTOs
│   │   │       │   ├── ReceiptResponse.java
│   │   │       │   ├── UploadReceiptResponse.java
│   │   │       │   └── ErrorResponse.java
│   │   │       ├── exception/        # Exception handling
│   │   │       │   └── GlobalExceptionHandler.java
│   │   │       ├── HomeController.java
│   │   │       └── ReceiptController.java
│   │   └── resources/
│   │       ├── templates/            # Thymeleaf templates
│   │       │   ├── home.html
│   │       │   ├── index.html
│   │       │   ├── login.html
│   │       │   └── layout/base.html
│   │       └── application.properties
│   └── test/                        # Test classes
│       └── java/com/gm/expensight/
│           ├── ExpensightApplicationTests.java
│           ├── service/
│           │   ├── FileValidatorTest.java
│           │   └── impl/
│           │       ├── ReceiptServiceImplTest.java
│           │       ├── TesseractOcrServiceTest.java
│           │       ├── ReceiptParserServiceImplTest.java
│           │       ├── OpenRouterLlmServiceTest.java
│           │       └── LocalFileStorageServiceTest.java
│           └── web/
│               ├── ReceiptControllerTest.java
│               └── HomeControllerTest.java
├── pom.xml
└── README.md
```

## Key Implementation Details

### Exception Handling

All exceptions extend `BaseException` and are handled by `GlobalExceptionHandler`:

- **ResourceNotFoundException** → 404 Not Found
- **ValidationException** → 400 Bad Request
- **UnauthorizedException** → 401 Unauthorized
- **ForbiddenException** → 403 Forbidden
- **OcrException, LlmException, FileStorageException** → 500 Internal Server Error
- **Generic Exception** → 500 Internal Server Error

### Currency Support

The system supports multiple currencies:
- **INR** (₹) - Default for Indian receipts
- **USD** ($)
- **EUR** (€)
- **GBP** (£)
- **JPY** (¥)
- Others: Currency code displayed

Currency symbols are automatically displayed in the UI based on the receipt's currency field.

### UI Features

- **Minimal Dashboard**: Shows user name and email in header
- **Table Layout**: Clean table showing:
  - Merchant name
  - Formatted date (e.g., "Dec 16, 2025")
  - Items as comma-separated list with quantities
  - Tax amount with currency symbol
  - Total amount with currency symbol
- **Responsive Design**: Works on different screen sizes

## Future Enhancements

- [ ] Support for multiple OAuth providers (GitHub, Microsoft) - Architecture ready
- [ ] Google Vision API integration as OCR alternative - Interface ready
- [ ] Async receipt processing with job queue
- [ ] Receipt editing and correction UI
- [ ] Export functionality (CSV, PDF reports)
- [ ] Receipt categorization and tagging
- [ ] Receipt search and filtering
- [ ] Receipt analytics and insights

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Contact

For questions or issues, please reach out on email `malleshguglothms@gmail.com`
