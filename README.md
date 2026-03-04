# The Orphans

Before running you must take the .env file I sent and add it to your local code directory. Pull the project and then add the .env file to the same directory as the pom file and src folder. In intellij go into edit config in the dropdown next to the run button and select the file in the environment variables box.

When the app is running it is currently on http://localhost:8080. HTML files are being stored in src/main/resources/static/. To access the frontend you can go to http://localhost:8080/index.html, http://localhost:8080/student_Login.html, http://localhost:8080/teacher-Login.html, etc.

You can reset the storage by running the below in the intellij terminal:

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/seed/reset-and-seed" `
  -ContentType "application/json" `
  -Body '{"confirm":"RESET"}'
  
It must provide -Body '{"confirm":"RESET"}' as a safeguard to ensure storage does not accidentally get wiped.

The health checks can be ran in the same way with:

Invoke-RestMethod "http://localhost:8080/health"
Invoke-RestMethod "http://localhost:8080/health/db"
Invoke-RestMethod "http://localhost:8080/health/blob"

health checks the server itself is running, health/db confirms the Neon DB is online + working, and health/blob uploads a small blob and reads it back to confirm Azure is online + working.