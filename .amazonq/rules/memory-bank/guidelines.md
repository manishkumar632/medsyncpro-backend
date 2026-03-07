# Development Guidelines

## Code Quality Standards

### Java Backend Conventions

#### Package Organization
- Follow domain-driven package structure: `com.medsyncpro.{layer}.{feature}`
- Layers: `config`, `controller`, `dto`, `entity`, `event`, `exception`, `filter`, `mapper`, `repository`, `response`, `service`, `utils`
- Keep related classes together (e.g., `dto/request`, `dto/response`, `dto/doctor`)

#### Naming Conventions
- **Classes**: PascalCase with descriptive suffixes
  - Controllers: `*Controller` (e.g., `AuthController`, `ProfileController`)
  - Services: `*Service` with implementations in `impl/*ServiceImpl`
  - Repositories: `*Repository`
  - Entities: Noun without suffix (e.g., `User`, `Doctor`, `Appointment`)
  - DTOs: `*Request`, `*Response`
  - Exceptions: `*Exception`
- **Methods**: camelCase with verb prefixes
  - `get*`, `fetch*`, `find*` for retrieval
  - `create*`, `save*`, `add*` for creation
  - `update*`, `modify*` for updates
  - `delete*`, `remove*` for deletion
  - `validate*`, `check*` for validation
- **Variables**: camelCase, descriptive names
- **Constants**: UPPER_SNAKE_CASE for static final fields

#### File Structure Standards
- One public class per file
- File name matches class name exactly
- Enums can be standalone files (e.g., `FileType.java`, `Status.java`)
- Group related enums by domain (e.g., `AppointmentStatus`, `VerificationStatus`)

### Frontend Conventions

#### File Naming
- **Components**: PascalCase for component files (e.g., `DoctorSidebar.jsx`, `PatientProfileClient.jsx`)
- **Pages**: lowercase with hyphens for route files (e.g., `page.jsx`, `layout.jsx`)
- **Styles**: kebab-case matching component (e.g., `doctor-settings.css`, `patient-dashboard.css`)
- **Actions**: camelCase with `Action` suffix (e.g., `doctorVerificationAction.ts`)
- **Utilities**: camelCase (e.g., `axios.ts`, `config.ts`)

#### Component Organization
- Use "use client" directive for client components
- Group related components in feature folders
- Separate page components from reusable components
- Co-locate styles with components

#### Naming Patterns
- **Functions**: camelCase with descriptive verbs (e.g., `handleUpload`, `loadData`, `fetchDoctorProfileData`)
- **Event Handlers**: `handle*` prefix (e.g., `handleSave`, `handleProfileChange`)
- **State Variables**: descriptive nouns (e.g., `loading`, `docTypes`, `verStatus`)
- **Boolean Variables**: `is*`, `has*`, `can*`, `should*` prefixes (e.g., `isUploading`, `canSubmit`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `ALLOWED_TYPES`, `MAX_FILE_SIZE`, `DOC_STATUS_MAP`)

## Architectural Patterns

### Backend Patterns

#### Layered Architecture
```
Controller → Service → Repository → Database
     ↓          ↓
    DTO      Entity
```

**Controller Layer**:
- Handle HTTP requests/responses
- Validate input with `@Valid`
- Delegate business logic to services
- Return `ResponseEntity<ApiResponse<T>>`
- Use `@RestController`, `@RequestMapping`

**Service Layer**:
- Implement business logic
- Use `@Service` annotation
- Interface + implementation pattern (`service/` + `service/impl/`)
- Handle transactions with `@Transactional`
- Throw `BusinessException` for business rule violations

**Repository Layer**:
- Extend `JpaRepository<Entity, ID>`
- Define custom query methods
- Use `@Query` for complex queries
- Keep data access logic isolated

#### Dependency Injection
- Use constructor injection with `@RequiredArgsConstructor` (Lombok)
- Mark dependencies as `private final`
- Avoid field injection

Example:
```java
@RestController
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final JwtService jwtService;
    // ...
}
```

#### Entity Design
- Extend `BaseEntity` for common fields (timestamps, soft delete)
- Use `@Entity`, `@Table` with indexes
- Apply Lombok annotations: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`
- Use `@SQLDelete` and `@SQLRestriction` for soft deletes
- UUID primary keys: `@GeneratedValue(strategy = GenerationType.UUID)`
- Enums with `@Enumerated(EnumType.STRING)`
- Builder pattern with `@Builder.Default` for default values

Example:
```java
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@SQLDelete(sql = "UPDATE users SET deleted = true WHERE id=?")
@SQLRestriction("deleted = false")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Email
    @Column(unique = true, nullable = false)
    private String email;
    
    @Builder.Default
    @Column(nullable = false)
    private boolean emailVerified = false;
}
```

#### Exception Handling
- Use `@RestControllerAdvice` for global exception handling
- Custom exceptions extend `RuntimeException`
- Return consistent error responses via `ApiResponse`
- Log errors with appropriate levels

#### Event-Driven Architecture
- Publish domain events for cross-cutting concerns
- Use Spring's `ApplicationEventPublisher`
- Create event classes (e.g., `UserSignupEvent`, `AppointmentBookedEvent`)
- Handle events with `@EventListener` in listener classes
- Keep event handling asynchronous where appropriate

### Frontend Patterns

#### Component Structure
- Functional components with hooks
- Extract reusable logic into custom hooks
- Use React Context for global state (Auth, Notifications)
- Server Actions for data mutations
- Client-side state with `useState`, `useEffect`

#### State Management
- Local state for component-specific data
- Context API for shared state (authentication, theme)
- Server state via Server Actions
- Form state with React Hook Form + Zod validation

#### Data Fetching
- Server Actions for mutations (e.g., `submitDoctorVerificationAction`)
- Fetch API with credentials for authenticated requests
- Axios instance with base URL and credentials
- Handle loading, error, and success states explicitly

Example:
```javascript
const [loading, setLoading] = useState(true);
const [data, setData] = useState(null);

const loadData = useCallback(async () => {
    setLoading(true);
    try {
        const result = await fetchDataAction();
        if (result.success) setData(result.data);
    } catch (e) {
        console.error("Failed to load", e);
    }
    setLoading(false);
}, []);

useEffect(() => { loadData(); }, [loadData]);
```

#### Form Handling
- Controlled components for form inputs
- Separate form state from display state
- Validate on client before submission
- Use FormData for file uploads
- Handle submission with async functions

#### File Upload Pattern (Cloudinary)
1. Request upload signature from backend
2. Upload directly to Cloudinary with signature
3. Save metadata to backend
4. Handle errors at each step
5. Show loading states during upload

## Common Code Idioms

### Backend Idioms

#### API Response Wrapper
```java
return ResponseEntity.ok(
    ApiResponse.success(data, "Operation successful")
);

return ResponseEntity.status(HttpStatus.CREATED)
    .body(ApiResponse.success(data, "Created successfully"));
```

#### Cookie Management
```java
ResponseCookie cookie = ResponseCookie.from("token", value)
    .httpOnly(true)
    .secure(true)
    .path("/")
    .maxAge(expiration / 1000)
    .sameSite("None")
    .build();
response.addHeader("Set-Cookie", cookie.toString());
```

#### File Validation
```java
private void validateFile(MultipartFile file, String[] allowedTypes, long maxSize) {
    if (file == null || file.isEmpty()) {
        throw new BusinessException("FILE_EMPTY", "File is empty");
    }
    if (file.getSize() > maxSize) {
        throw new BusinessException("FILE_TOO_LARGE", "File too large");
    }
    String contentType = file.getContentType();
    if (!Arrays.asList(allowedTypes).contains(contentType)) {
        throw new BusinessException("INVALID_FILE_TYPE", "Invalid type");
    }
}
```

#### Logging Pattern
```java
@Slf4j
public class MyService {
    public void doSomething() {
        log.info("Starting operation");
        log.error("Error occurred", exception);
        log.warn("Warning message");
    }
}
```

### Frontend Idioms

#### Toggle Component Pattern
```javascript
function Toggle({ checked, onChange }) {
    return (
        <button
            type="button"
            className={`toggle ${checked ? "active" : ""}`}
            onClick={() => onChange(!checked)}
            role="switch"
            aria-checked={checked}
        >
            <span className="toggle-thumb" />
        </button>
    );
}
```

#### Conditional Rendering
```javascript
{loading && <Loader />}
{error && <ErrorMessage />}
{data && <DataDisplay data={data} />}

{items.length > 0 ? (
    <List items={items} />
) : (
    <EmptyState />
)}
```

#### Dynamic Class Names
```javascript
className={`card ${isActive ? "active" : ""} ${isDisabled ? "disabled" : ""}`}
```

#### Event Handler Pattern
```javascript
const handleChange = (key, value) => {
    setState(prev => ({ ...prev, [key]: value }));
    setDirty(true);
};

onChange={(e) => handleChange("name", e.target.value)}
```

#### API Helper Function
```javascript
async function api(path, options = {}) {
    const res = await fetch(`${config.apiUrl}${path}`, {
        credentials: "include",
        headers: { "Content-Type": "application/json", ...options.headers },
        ...options,
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.message || "Request failed");
    return data.data;
}
```

## Frequently Used Annotations

### Backend Annotations

#### Spring Framework
- `@RestController` - REST API controller
- `@RequestMapping("/path")` - Base path for controller
- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping` - HTTP method mappings
- `@RequestBody` - Bind request body to parameter
- `@RequestParam` - Bind query parameter
- `@PathVariable` - Bind path variable
- `@Valid` - Enable validation
- `@Service` - Service layer component
- `@Repository` - Data access layer component
- `@Configuration` - Configuration class
- `@Bean` - Bean definition
- `@Value("${property}")` - Inject property value
- `@Transactional` - Transaction boundary
- `@EventListener` - Event handler method
- `@Async` - Asynchronous execution
- `@Primary` - Primary bean when multiple candidates exist

#### JPA/Hibernate
- `@Entity` - JPA entity
- `@Table(name = "table_name", indexes = {...})` - Table mapping
- `@Id` - Primary key
- `@GeneratedValue(strategy = GenerationType.UUID)` - ID generation
- `@Column(nullable = false, unique = true)` - Column mapping
- `@Enumerated(EnumType.STRING)` - Enum mapping
- `@ManyToOne`, `@OneToMany`, `@ManyToMany`, `@OneToOne` - Relationships
- `@JoinColumn` - Foreign key column
- `@Version` - Optimistic locking
- `@SQLDelete` - Custom delete SQL
- `@SQLRestriction` - Filter condition

#### Validation
- `@Email` - Email validation
- `@NotNull`, `@NotBlank`, `@NotEmpty` - Null/empty checks
- `@Size(min = x, max = y)` - Size constraints
- `@Min`, `@Max` - Numeric constraints
- `@Pattern(regexp = "...")` - Regex validation

#### Lombok
- `@Getter`, `@Setter` - Generate getters/setters
- `@NoArgsConstructor`, `@AllArgsConstructor` - Generate constructors
- `@RequiredArgsConstructor` - Constructor for final fields
- `@Builder` - Builder pattern
- `@Builder.Default` - Default value in builder
- `@Slf4j` - Logger field
- `@Data` - All-in-one (getter, setter, toString, equals, hashCode)

### Frontend Patterns

#### React Hooks
- `useState` - Local state
- `useEffect` - Side effects
- `useCallback` - Memoized callback
- `useRef` - Mutable reference
- `useContext` - Context consumption
- `useActionState` - Server action state (Next.js)

#### Custom Hooks
- `useAuth()` - Authentication context
- Custom data fetching hooks

## Best Practices

### Security
- Use JWT with HttpOnly cookies for authentication
- Validate all inputs on both client and server
- Implement CORS properly
- Sanitize file uploads (type, size validation)
- Use signed uploads for cloud storage
- Implement token blacklisting for logout
- Use HTTPS in production (secure cookies)

### Performance
- Use indexes on frequently queried columns
- Implement pagination for large datasets
- Lazy load components where appropriate
- Optimize images and assets
- Use CDN for static files (Cloudinary)
- Cache frequently accessed data

### Error Handling
- Always handle errors gracefully
- Provide meaningful error messages to users
- Log errors with context for debugging
- Use try-catch blocks for async operations
- Implement global error handlers

### Testing
- Write unit tests for business logic
- Test edge cases and error scenarios
- Mock external dependencies
- Use `@SpringBootTest` for integration tests

### Code Organization
- Keep files focused and single-purpose
- Extract reusable logic into utilities
- Use constants for magic values
- Comment complex logic, not obvious code
- Keep functions small and focused

### Git Practices
- Write descriptive commit messages
- Use feature branches
- Review code before merging
- Keep commits atomic and focused
