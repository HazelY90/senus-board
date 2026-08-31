# 1. Requirements Analysis

- Analyse the assignment document to identify the product goals, required capabilities, constraints, and deliverables.
- Review the Senus investor relations website and its source documents to determine what financial and operating data are available.
- Verify the available reporting periods, metrics, source locations, and data limitations.
- Identify the target users and define the financial information most relevant to each user type.
- Decide which metrics, comparisons, insights, and source references the Board Report should display.
- Record the detailed findings in [Requirements.md](Requirements.md) to guide the data model, backend APIs, AI extraction workflow, and frontend design.

# 2. Technology Stack Selection

- **Frontend:** TypeScript, Next.js, and React
- **Backend:** Java, Spring Boot, and REST APIs
- **Database:** MySQL
- **Cloud:** AWS EC2 for application deployment and Amazon RDS for MySQL
- **AI:** OpenAI API for structured financial data extraction and Board-level commentary
- **Document Processing:** Apache PDFBox for extracting text from PDF documents, subject to validation against the available source files
- **Charts:** Recharts for interactive financial visualisations, subject to validation during frontend development

The application will use a single repository containing the frontend and backend. The main application stack will remain Java and TypeScript unless a clear technical requirement justifies another runtime or service.

# 3. Frontend Design

- Define the data, comparisons, context, and provenance that each frontend category needs to display before designing the persistence model.
- Select an appropriate presentation format for each type of data, including KPI cards, paired-period comparisons, composition charts, calculation visuals, target indicators, and detail tables.
- Define category-specific response structures for Growth, Profitability, Liquidity, and Capital based on the selected frontend components.
- Use these frontend data requirements to determine the backend API contracts, database entities, relationships, and stored attributes.
- Record the detailed design decisions in [FrontendDesign.md](FrontendDesign.md) to guide database and backend development.

# 4. API Design

- Derive the backend data contracts from the data required by the four frontend categories.
- Define independent single-period endpoints for Growth, Profitability, Liquidity, and Capital so the frontend can request category data in parallel.
- Define common response objects for reporting periods, metric values, value classifications, units, comments, and source references.
- Keep period comparison logic in the frontend by allowing it to request the same category for two equivalent periods.
- Record the endpoints, response templates, and response rules in [APIDesign.md](APIDesign.md) to guide database and backend development.

# 5. Database Design

- Derive the persistence requirements from the single-period API contracts without copying API DTOs directly into database tables.
- Separate reporting periods, metrics metadata, dimensions, source documents, validated metric values, strategic targets, and AI extraction staging data, while storing calculation metadata directly with each metric value.
- Store scalar and breakdown values in a shared metric model so the four frontend categories can be assembled without duplicate category tables.
- Store category and unit metadata in metrics, while preserving each value's source, classification, explanatory comments, and validation state.
- Record the detailed schema and API mapping in [DatabaseDesign.md](DatabaseDesign.md) to guide Entity, repository, and service development.

# 6. Backend Development

- Implement the database migration based on [DatabaseDesign.md](DatabaseDesign.md).
- Design and implement the AI extraction workflow and extraction rules documented in [AIExtractionJob.md](AIExtractionJob.md).
- Design and implement user authentication based on [AuthenticationDesign.md](AuthenticationDesign.md).
- Complete the backend API implementation according to the contracts and response rules defined in [APIDesign.md](APIDesign.md).
- Adopt a clear Controller-Service-Entity-Database layered architecture to keep the application maintainable and extensible.

# 7. Frontend Development

- Define the frontend pages, access rules, shared layouts, and routing flow before implementation, and record the final decisions in [FrontendDesignFinal.md](FrontendDesignFinal.md).
- Build the Welcome, Period Reports, Comparison, Documents, and Administration experiences according to the final frontend design and connect them to the completed backend APIs.
- Organise the frontend into App Router pages and layouts, feature modules, reusable components, contexts and hooks, API services, shared types, and utility functions.
- Keep page rendering, feature state, backend communication, and shared data definitions in separate layers so the frontend remains maintainable and scalable as new pages and capabilities are added.

# 8. Cloud Deployment

- Deploy the production frontend and backend builds to an Ubuntu AWS EC2 instance using environment-specific configuration.
- Deploy MySQL to Amazon RDS, apply the database migrations, and protect access with appropriate security group rules.
- Manage the frontend and backend with systemd to support automatic startup, failure recovery, and centralised logging.
- Configure Nginx as the public entry point and reverse proxy while keeping the application services private.
