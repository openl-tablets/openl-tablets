# Studio UI

## Project source structure

- `components/`: This directory contains shared or generic UI components that can be used across your application.
- `constants/`: This directory contains constants that are used across your application.
- `containers/`: This directory is used to group your stateful container components if you're using Redux or a similar state management library.
- `hooks/`: This directory contains custom React hooks.
- `layouts/`: This directory contains components that dictate the major page structure.
- `pages/`: This directory contains components that are directly used as routes. These are the top-level components, each corresponding to a different route in your application.
- `routes/`: This directory contains all the route definitions for your application.
- `services/`: This directory contains service utilities, such as API clients or other services that you use to perform the main business logic.
- `store/`: This directory contains Redux-specific pieces, such as actions and reducers.
- `utils/`: This directory contains utility functions that can be used across your application.

### Installing NodeJS.

To run the Studio UI, you need to have Node.js installed. You can download it from [Node.js official website](https://nodejs.org/).
The required version is Node.js 24.

### Installing dependencies.

```bash
  npm install
```

### Running the development server.

```bash
  npm run start
```

### Docker configuration for development mode

1. Open `compose.yaml`
2. Uncomment next lines
```yaml
_REACT_UI_ROOT_: http://localhost:3100
```

### Build and Run OpenL Studio Backend

```bash
  mvn -DskipTests -T1C
  docker compose up --build
```
