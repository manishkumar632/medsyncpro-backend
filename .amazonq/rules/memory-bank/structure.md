# Project Structure

## Workspace Organization
This is a multi-package workspace containing:
- **medsyncpro-backend**: Spring Boot REST API backend
- **online-medication/frontend/medsyncpro**: Next.js frontend application

## Backend Structure (medsyncpro-backend)

### Directory Layout
```
src/main/java/com/medsyncpro/
├── config/              # Application configuration
├── controller/          # REST API endpoints
├── dto/                 # Data Transfer Objects
├── entity/              # JPA database entities
├── event/               # Event-driven architecture components
├── exception/           # Exception handling
├── filter/              # Security filters
├── mapper/              # Entity-DTO mapping
├── repository/          # Data access layer
├── response/            # API response wrappers
├── service/             # Business logic layer
│   └── impl/            # Service implementations
└── utils/               # Utility classes
```

### Core Components

#### Configuration Layer (`config/`)
- **SecurityConfig**: Spring Security, JWT, CORS configuration
- **AsyncConfig**: Asynchronous processing setup
- **FirebaseInitializer**: Firebase Cloud Messaging initialization
- **DataInitializer**: Database seeding and initial data setup
- **CorsConfig**: Cross-origin resource sharing policies

#### Controller Layer (`controller/`)
- **AuthController**: Authentication endpoints (register, login, verify, logout)
- **ProfileController**: User profile management (GET, PATCH)
- **DoctorController**: Doctor-specific operations
- **DoctorSettingsController**: Doctor preferences and settings
- **DocumentTypeController**: Document type management
- **PatientController**: Patient-specific operations
- **PharmacyController**: Pharmacy operations
- **AdminController**: Administrative functions
- **NotificationController**: Notification management
- **DoctorSearchController**: Doctor search and filtering
- **PublicQueryController**: Unauthenticated public endpoints
- **FileController**: File serving (deprecated with Cloudinary)

#### Entity Layer (`entity/`)
Core entities:
- **User**: Base user entity with role-based inheritance
- **Doctor**: Medical professional with specialities and clinics
- **Patient**: Patient information and medical history
- **Pharmacy**: Pharmacy details and inventory
- **Admin**: Administrative user
- **Agent**: Verification agent
- **Appointment**: Booking and consultation records
- **Prescription**: Medication prescriptions
- **Document**: File metadata for uploaded documents
- **DocumentType**: Document type definitions
- **VerificationRequest**: Doctor verification workflow
- **Notification**: User notifications
- **AuditLog**: System audit trail
- **RefreshToken**: JWT refresh tokens
- **BlacklistedToken**: Invalidated tokens

Enums:
- **Role**: USER_ROLES (ADMIN, DOCTOR, PATIENT, PHARMACIST, AGENT)
- **Status**: Entity status (ACTIVE, INACTIVE, DELETED)
- **VerificationStatus**: Verification states
- **AppointmentStatus**: Appointment lifecycle states
- **FileType**: Document categories
- **Gender**: User gender options
- **ConsultationType**: Consultation modes

#### Service Layer (`service/` & `service/impl/`)
- **AuthService**: Authentication and authorization logic
- **ProfileService**: Profile update with transaction management
- **DoctorService**: Doctor operations and verification
- **DoctorSettingsService**: Doctor preference management
- **PatientService**: Patient management
- **PharmacyService**: Pharmacy operations
- **AdminService**: Administrative functions
- **VerificationService**: Document verification workflow
- **FileStorageService**: Cloudinary integration
- **EmailService**: Email notifications via Brevo
- **FirebasePushService**: Push notifications
- **SseEmitterService**: Server-Sent Events for real-time updates
- **JwtService**: JWT token generation and validation
- **RefreshTokenService**: Refresh token management
- **TokenBlacklistService**: Token invalidation
- **AuditLogService**: Audit logging
- **OnlyQueryService**: Public query operations
- **DoctorSearchService**: Search and filtering

#### Repository Layer (`repository/`)
Spring Data JPA repositories for all entities with custom query methods

#### Event Layer (`event/`)
Event-driven architecture:
- **UserSignupEvent**: Triggered on user registration
- **AppointmentBookedEvent**: Appointment creation
- **AppointmentCancelledEvent**: Appointment cancellation
- **DocumentSubmittedEvent**: Document upload
- **VerificationDecisionEvent**: Verification approval/rejection
- **AppointmentEventListener**: Handles appointment events
- **VerificationEventListener**: Handles verification events

#### Exception Layer (`exception/`)
- **GlobalExceptionHandler**: Centralized exception handling
- **BusinessException**: Custom business logic exceptions
- **ResourceNotFoundException**: 404 error handling

#### DTO Layer (`dto/`)
Request and response DTOs organized by feature:
- `dto/request/`: Incoming request objects
- `dto/response/`: Outgoing response objects
- `dto/doctor/`: Doctor-specific DTOs

## Frontend Structure (online-medication/frontend/medsyncpro)

### Directory Layout
```
app/
├── (root)/              # Landing page
├── auth/                # Authentication pages
├── admin/               # Admin dashboard and features
├── agent/               # Agent verification interface
├── doctor/              # Doctor portal
├── patient/             # Patient portal
├── pharmacy/            # Pharmacy dashboard
├── api/                 # API route handlers (Next.js)
├── components/          # Shared UI components
└── context/             # React context providers

actions/                 # Server actions
components/              # Reusable components
hooks/                   # Custom React hooks
lib/                     # Utility libraries
public/                  # Static assets
```

### Key Frontend Components

#### Role-Based Dashboards
- **Admin Dashboard**: User management, verification queue, analytics
- **Doctor Dashboard**: Appointments, patients, settings, analytics
- **Patient Dashboard**: Find doctors, book appointments, view prescriptions
- **Pharmacy Dashboard**: Sales overview, prescription fulfillment
- **Agent Dashboard**: Verification review interface

#### Shared Components
- **RouteGuard**: Role-based route protection
- **ProfessionalVerificationGuard**: Doctor verification status check
- **Header/Footer**: Common layout components
- **NotificationContext**: Real-time notification management
- **AuthContext**: Authentication state management

#### Feature Modules
- **Appointment Management**: Booking, viewing, cancellation
- **Doctor Search**: Filtering, sorting, profile viewing
- **Settings**: Profile updates, preferences, notifications
- **Verification**: Document upload, status tracking

## Architectural Patterns

### Backend Architecture
- **Layered Architecture**: Controller → Service → Repository
- **Clean Architecture**: Separation of concerns, dependency inversion
- **Event-Driven**: Asynchronous event handling for notifications
- **Transaction Management**: ACID compliance with rollback support
- **Optimistic Locking**: Concurrent update handling with @Version
- **DTO Pattern**: Entity-DTO separation for API contracts
- **Repository Pattern**: Data access abstraction
- **Service Layer Pattern**: Business logic encapsulation

### Frontend Architecture
- **App Router**: Next.js 14+ file-based routing
- **Server Actions**: Form handling and data mutations
- **Context API**: Global state management
- **Component Composition**: Reusable UI components
- **Route Guards**: Authentication and authorization checks
- **CSS Modules**: Scoped styling per component

## Data Flow

### Authentication Flow
1. User submits credentials → AuthController
2. AuthService validates → UserRepository
3. JwtService generates tokens
4. HttpOnly cookie set with JWT
5. RefreshToken stored in database

### Profile Update Flow
1. Client sends multipart/form-data → ProfileController
2. ProfileService validates input
3. FileStorageService uploads to Cloudinary
4. Transaction begins
5. User entity updated → UserRepository
6. Transaction commits or rolls back
7. Response with updated profile

### Verification Flow
1. Doctor uploads documents → DoctorController
2. DocumentSubmittedEvent published
3. VerificationEventListener sends notification
4. Agent/Admin reviews → AdminController
5. VerificationDecisionEvent published
6. Doctor notified via email/push
7. Status updated in database

### Appointment Flow
1. Patient books → PatientController
2. AppointmentBookedEvent published
3. AppointmentEventListener notifies doctor
4. Doctor confirms/rejects
5. Status updated
6. Both parties notified
