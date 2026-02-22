# medsyncpro-backend

## Features

### Authentication
- User registration with role-based access (ADMIN, DOCTOR, PATIENT, PHARMACIST)
- Email verification with token
- Login with JWT authentication (HttpOnly cookies)
- Logout functionality

### Profile Management ⭐ NEW
- **Update Profile API** - Single PATCH endpoint for profile updates
- **Partial Updates** - Update only the fields you need
- **File Uploads** - Profile image and multiple documents
- **Cloud Storage** - Files stored on Cloudinary CDN ☁️
- **Transaction Safety** - Atomic updates with rollback
- **Security** - JWT-based authentication, self-update only
- **Validation** - Comprehensive input and file validation

## Quick Start

### Run Application
```bash
mvn spring-boot:run
```

### Update Profile
```bash
curl -X PATCH http://localhost:8080/api/users/profile \
  -F 'profile={"name":"John Doe","phone":"9876543210"}' \
  -F "profileImage=@profile.jpg" \
  --cookie "token=JWT_TOKEN"
```

## Documentation

### Profile Update Feature
- **[INDEX.md](INDEX.md)** - Documentation index and navigation
- **[FINAL_SUMMARY.md](FINAL_SUMMARY.md)** - Quick overview and reference
- **[CLOUDINARY_MIGRATION.md](CLOUDINARY_MIGRATION.md)** - Cloudinary integration guide ☁️
- **[PROFILE_UPDATE_README.md](PROFILE_UPDATE_README.md)** - Quick start guide
- **[PROFILE_UPDATE_ARCHITECTURE.md](PROFILE_UPDATE_ARCHITECTURE.md)** - Architecture and design
- **[COMPLETE_CURL_TESTS.md](COMPLETE_CURL_TESTS.md)** - 48 test commands
- **[VISUAL_FLOW_DIAGRAMS.md](VISUAL_FLOW_DIAGRAMS.md)** - Visual flows

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user
- `POST /api/auth/verify-email` - Verify email
- `POST /api/auth/resend-verification` - Resend verification email
- `POST /api/auth/logout` - Logout user

### Profile Management
- `GET /api/users/profile` - Get current user profile
- `PATCH /api/users/profile` - Update profile (multipart/form-data)

### File Serving
- Files served directly from Cloudinary CDN

## Tech Stack

- **Framework**: Spring Boot 3.x
- **Security**: Spring Security + JWT
- **Database**: MySQL with JPA/Hibernate
- **Validation**: Jakarta Validation
- **Email**: Brevo API
- **File Storage**: Cloudinary Cloud Storage ☁️

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/medsyncpro
spring.datasource.username=root
spring.datasource.password=password

# JWT
jwt.secret=YOUR_SECRET_KEY
jwt.expiration=86400000

# Cloudinary
cloudinary.cloud-name=dhopew3ev
cloudinary.api-key=337429495835427
cloudinary.api-secret=t7yopXsId9npZCI2BrBKfdcodV0

# File Upload
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB

# Email
brevo.api.key=YOUR_BREVO_API_KEY
brevo.from.email=noreply@medsyncpro.com
```

## Testing

### Run Automated Tests
```bash
chmod +x test_profile_api.sh
./test_profile_api.sh
```

### Manual Testing
See [COMPLETE_CURL_TESTS.md](COMPLETE_CURL_TESTS.md) for 48 test commands

## Project Structure

```
src/main/java/com/medsyncpro/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── dto/             # Data Transfer Objects
├── entity/          # JPA entities
├── exception/       # Exception handling
├── filter/          # Security filters
├── mapper/          # Entity-DTO mappers
├── repository/      # Data repositories
├── response/        # Response wrappers
└── service/         # Business logic
    └── impl/        # Service implementations
```

## Features Implemented

✅ User registration with validation
✅ Email verification
✅ JWT authentication with HttpOnly cookies
✅ Role-based access control
✅ Profile management with partial updates
✅ File upload (images and documents)
✅ Cloudinary cloud storage integration ☁️
✅ Transaction management with rollback
✅ Optimistic locking for concurrent updates
✅ Comprehensive validation
✅ Global exception handling
✅ File storage abstraction

## Production-Ready Features

✅ Clean Architecture (Controller → Service → Repository)
✅ Transaction Management (ACID compliance)
✅ Optimistic Locking (Concurrent update handling)
✅ Partial Updates (PATCH semantics)
✅ File Rollback (Cleanup on failure)
✅ Comprehensive Validation (Multiple layers)
✅ Security (JWT, file validation)
✅ Cloud Storage (Cloudinary CDN) ☁️
✅ Abstraction (Storage provider)
✅ Error Handling (Comprehensive)
✅ Documentation (Extensive)
✅ Extensibility (Easy to extend)
✅ Testability (Well-structured)

## License

MIT