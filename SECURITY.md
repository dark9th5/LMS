# Security Policy

- Never commit passwords, private keys, license signing keys, database credentials, or customer data.
- Production deployments must replace all values from `.env.example`.
- The API Gateway and every business service validate authorization independently.
- Databases are not exposed to the customer LAN by default.
- File uploads are allow-listed by MIME type and size; production should add an internal malware scanner.
- Report security issues privately to the repository owner. Do not open a public issue containing exploit details or customer information.
