# Technology Stack

## Backend Technologies

### Core Framework
- **Spring Boot**: 4.1.0-M2 (Milestone Release)
- **Java**: 21 (LTS)
- **Build Tool**: Apache Maven

### Spring Ecosystem
- **Spring Boot Starter Web MVC**: REST API development
- **Spring Boot Starter Data JPA**: Database ORM with Hibernate
- **Spring Boot Starter Security**: Authentication and authorization
- **Spring Boot Starter Validation**: Jakarta Bean Validation
- **Spring Boot Starter AMQP**: RabbitMQ messaging
- **Spring Boot Starter Mail**: Email functionality
- **Spring Boot Starter WebFlux**: Reactive web client
- **Spring Boot Starter Actuator**: Application monitoring and health checks
- **Spring Boot DevTools**: Hot reload during development

### Security & Authentication
- **Spring Security**: Core security framework
- **JWT (JSON Web Tokens)**: Stateless authentication
  - `jjwt-api`: 0.13.0
  - `jjwt-impl`: 0.13.0
  - `jjwt-jackson`: 0.13.0
- **HttpOnly Cookies**: Secure token storage

### Database
- **PostgreSQL**: Primary relational database
- **Spring Data JPA**: Data access abstraction
- **Hibernate**: ORM implementation

### Cloud Services
- **Cloudinary**: Cloud-based file storage and CDN
  - `cloudinary-http44`: 1.38.0
- **Firebase Admin SDK**: Push notifications
  - `firebase-admin`: 9.2.0

### Utilities & Libraries
- **Lombok**: Boilerplate code reduction
- **Jackson Databind**: JSON serialization/deserialization
  - `jackson-databind`: 3.1.0-rc1 (tools.jackson.core)
  - `jackson-databind`: (com.fasterxml.jackson.core)
- **Spring Dotenv**: Environment variable management
  - `spring-dotenv`: 4.0.0

### Email Service
- **Brevo API**: Transactional email delivery

### Testing
- **Spring Boot Starter Test**: Testing framework
- **Spring Security Test**: Security testing utilities
- **Spring Boot Actuator Test**: Actuator endpoint testing
- **Spring Boot Validation Test**: Validation testing
- **Spring Boot WebMVC Test**: Controller testing

## Frontend Technologies

### Core Framework
- **Next.js**: 16.1.6 (React framework with App Router)
- **React**: 19.2.3
- **React DOM**: 19.2.3
- **TypeScript**: 5.x
- **Node.js**: 20.x

### UI & Styling
- **Tailwind CSS**: 4.x (Utility-first CSS)
- **@tailwindcss/postcss**: 4.x
- **PostCSS**: CSS processing
- **Radix UI**: Accessible component primitives
- **Lucide React**: Icon library (0.575.0)
- **React Icons**: Additional icon set (5.5.0)
- **Class Variance Authority**: Component variant management (0.7.1)
- **clsx**: Conditional className utility (2.1.1)
- **Tailwind Merge**: Merge Tailwind classes (3.5.0)
- **tw-animate-css**: Animation utilities (1.4.0)

### UI Components & Libraries
- **shadcn/ui**: Component library (3.8.5)
- **cmdk**: Command menu component (1.1.1)
- **Framer Motion**: Animation library (12.34.3)
- **Vaul**: Drawer component (1.1.2)
- **Embla Carousel React**: Carousel component (8.6.0)
- **React Resizable Panels**: Resizable panel layouts (4.6.5)

### Form Management
- **React Hook Form**: 7.71.2
- **@hookform/resolvers**: Form validation resolvers (5.2.2)
- **Zod**: Schema validation (4.3.6)
- **Input OTP**: OTP input component (1.4.2)

### Data & State Management
- **Axios**: HTTP client (1.13.6)
- **@tanstack/react-table**: Table component (8.21.3)
- **React Router DOM**: Client-side routing (7.13.1)

### Date & Time
- **date-fns**: Date utility library (4.1.0)
- **React Day Picker**: Date picker component (9.13.2)

### Notifications & UI Feedback
- **Sonner**: Toast notifications (2.0.7)

### Charts & Visualization
- **Recharts**: Chart library (2.15.4)

### Theming
- **next-themes**: Dark mode support (0.4.6)

### External Services
- **Firebase**: 12.9.0 (Client SDK for push notifications)
- **@sendgrid/mail**: Email service (8.1.6)

### Development Tools
- **ESLint**: 9.x (Code linting)
- **eslint-config-next**: Next.js ESLint configuration (16.1.6)

## Development Commands

### Backend
```bash
# Run application
mvn spring-boot:run

# Build project
mvn clean install

# Run tests
mvn test

# Package application
mvn package

# Skip tests during build
mvn clean install -DskipTests
```

### Frontend
```bash
# Development server
npm run dev

# Production build
npm run build

# Start production server
npm run start

# Lint code
npm run lint
```

## Configuration Files

### Backend
- **pom.xml**: Maven dependencies and build configuration
- **application.properties**: Main configuration file
- **application-dev.properties**: Development environment config
- **env.properties**: Environment variables

### Frontend
- **package.json**: NPM dependencies and scripts
- **next.config.ts**: Next.js configuration
- **tsconfig.json**: TypeScript configuration
- **jsconfig.json**: JavaScript configuration
- **eslint.config.mjs**: ESLint rules
- **postcss.config.mjs**: PostCSS configuration
- **components.json**: shadcn/ui configuration
- **.env.local**: Environment variables

## Environment Variables

### Backend Required
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/medsyncpro
spring.datasource.username=<username>
spring.datasource.password=<password>

# JWT
jwt.secret=<secret_key>
jwt.expiration=86400000

# Cloudinary
cloudinary.cloud-name=<cloud_name>
cloudinary.api-key=<api_key>
cloudinary.api-secret=<api_secret>

# Brevo Email
brevo.api.key=<api_key>
brevo.from.email=<email>

# Firebase
firebase.credentials.path=<path_to_json>
```

### Frontend Required
```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_FIREBASE_API_KEY=<key>
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=<domain>
NEXT_PUBLIC_FIREBASE_PROJECT_ID=<id>
```

## API Communication

### Backend API
- **Base URL**: `http://localhost:8080`
- **Content Types**: `application/json`, `multipart/form-data`
- **Authentication**: JWT in HttpOnly cookies
- **CORS**: Configured for frontend origin

### Frontend API Client
- **Axios**: Configured with base URL and credentials
- **Server Actions**: Next.js server-side data mutations
- **API Routes**: Next.js API endpoints for proxying

## Database Schema

### ORM Configuration
- **Hibernate DDL Auto**: update (development), validate (production)
- **Naming Strategy**: Snake case (physical naming)
- **Dialect**: PostgreSQL
- **Connection Pool**: HikariCP (default)

### Key Tables
- users, doctors, patients, pharmacies, admins, agents
- appointments, prescriptions
- documents, document_types
- verification_requests
- notifications
- audit_logs
- refresh_tokens, blacklisted_tokens
- verification_tokens

## Deployment

### Backend
- **Container**: Docker support (Dockerfile present)
- **Port**: 8080
- **Profile**: dev, prod

### Frontend
- **Platform**: Vercel (optimized for Next.js)
- **Build Output**: Static + Server-side rendering
- **Port**: 3000 (development)

## Version Control
- **Git**: Source control
- **GitHub**: Repository hosting
- **GitHub Actions**: CI/CD workflows (nextjs.yml)
