# studio-ui - React/TypeScript Frontend Conventions

**Module**: studio-ui (Modern React Frontend)
**Location**: `/home/user/openl-tablets/STUDIO/studio-ui/`
**Version**: 6.0.0-SNAPSHOT
**Last Updated**: 2025-11-05

---

## Module Purpose

The studio-ui module is the **modern React/TypeScript frontend** for OpenL Tablets Web Studio, providing:
- Rule project management UI
- Excel rule editing interface
- Repository management
- User administration
- Modern, responsive UI with Ant Design
- Internationalization (i18n)

**Status**: ✅ Active development - replacing legacy JSF UI

---

## Technology Stack

### Core Framework
- **React**: 18.3.1 (Functional components + Hooks)
- **TypeScript**: 5.8.3 (Strict mode enabled)
- **Node.js**: 24.9.0 (Build only, not runtime)

### UI Framework & Styling
- **Ant Design**: 5.26.4 (Component library)
- **SCSS/Sass**: 1.89.2 (CSS preprocessor)
- **CSS Modules**: Enabled for component styling

### State Management & Routing
- **Zustand**: 5.0.6 (State management)
- **React Router**: 7.6.3 (Client-side routing)
- **React Query** (TanStack Query): For server state (if used)

### Internationalization
- **i18next**: 25.3.1 (i18n framework)
- **react-i18next**: 16.1.2 (React integration)

### Build Tools
- **Webpack**: 5.100.2 (Module bundler)
- **Frontend Maven Plugin**: 1.15.4 (Maven integration)
- **TypeScript Compiler**: 5.8.3
- **ESLint**: Code linting
- **Prettier**: Code formatting

### HTTP Client
- **Axios**: For API calls (or fetch API)

---

## Project Structure

```text
studio-ui/
├── src/
│   ├── components/          # React components
│   │   ├── common/          # Shared/reusable components
│   │   ├── layout/          # Layout components (Header, Sidebar)
│   │   ├── projects/        # Project management components
│   │   ├── editor/          # Rule editor components
│   │   └── admin/           # Admin components
│   ├── pages/               # Page components (routed)
│   ├── store/               # Zustand stores
│   ├── api/                 # API client code
│   ├── hooks/               # Custom React hooks
│   ├── utils/               # Utility functions
│   ├── types/               # TypeScript type definitions
│   ├── i18n/                # Internationalization
│   │   ├── locales/
│   │   │   ├── en.json
│   │   │   └── ru.json
│   │   └── config.ts
│   ├── styles/              # Global styles
│   ├── App.tsx              # Root component
│   └── index.tsx            # Entry point
├── public/                  # Static assets
├── package.json             # npm dependencies
├── tsconfig.json            # TypeScript configuration
├── webpack.config.js        # Webpack configuration
├── .eslintrc.js            # ESLint configuration
├── .prettierrc             # Prettier configuration
└── pom.xml                  # Maven configuration
```

---

## Coding Conventions

### TypeScript

#### Type Safety
```typescript
// ✅ GOOD: Strict typing
interface Project {
  name: string;
  path: string;
  status: 'open' | 'closed' | 'locked';
  lastModified: Date;
}

function getProject(id: string): Promise<Project> {
  return api.get<Project>(`/api/projects/${id}`);
}

// ❌ BAD: Using 'any'
function getProject(id: any): Promise<any> {
  return api.get(`/api/projects/${id}`);
}
```

#### Avoid Type Assertions
```typescript
// ✅ GOOD: Type guard
function isProject(obj: unknown): obj is Project {
  return (
    typeof obj === 'object' &&
    obj !== null &&
    'name' in obj &&
    'path' in obj
  );
}

// ❌ BAD: Unsafe type assertion
const project = response.data as Project;
```

#### Use Discriminated Unions
```typescript
// ✅ GOOD: Discriminated union for API states
type ApiState<T> =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; error: string };

function handleState<T>(state: ApiState<T>) {
  switch (state.status) {
    case 'idle':
      return <div>Ready</div>;
    case 'loading':
      return <Spin />;
    case 'success':
      return <div>{state.data}</div>;
    case 'error':
      return <Alert message={state.error} type="error" />;
  }
}
```

### React Components

#### Functional Components with Hooks
```typescript
// ✅ GOOD: Functional component with TypeScript
import React, { useState, useEffect } from 'react';
import { Button, Card } from 'antd';

interface ProjectCardProps {
  project: Project;
  onOpen: (project: Project) => void;
  onDelete: (projectId: string) => void;
}

export const ProjectCard: React.FC<ProjectCardProps> = ({
  project,
  onOpen,
  onDelete,
}) => {
  const [isDeleting, setIsDeleting] = useState(false);

  const handleDelete = async () => {
    setIsDeleting(true);
    try {
      await onDelete(project.id);
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <Card title={project.name}>
      <p>Path: {project.path}</p>
      <Button onClick={() => onOpen(project)}>Open</Button>
      <Button
        danger
        loading={isDeleting}
        onClick={handleDelete}
      >
        Delete
      </Button>
    </Card>
  );
};
```

#### Component File Structure
```typescript
// MyComponent.tsx

// 1. Imports (grouped)
import React, { useState, useEffect } from 'react';
import { Button, Modal } from 'antd';
import { useTranslation } from 'react-i18next';

import { useProjectStore } from '@/store/projectStore';
import { ProjectApi } from '@/api/projectApi';
import styles from './MyComponent.module.scss';

// 2. Types/Interfaces
interface MyComponentProps {
  projectId: string;
  onClose: () => void;
}

// 3. Component
export const MyComponent: React.FC<MyComponentProps> = ({
  projectId,
  onClose,
}) => {
  // Hooks
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const project = useProjectStore((state) => state.currentProject);

  // Effects
  useEffect(() => {
    loadProject();
  }, [projectId]);

  // Handlers
  const loadProject = async () => {
    setLoading(true);
    try {
      await ProjectApi.load(projectId);
    } finally {
      setLoading(false);
    }
  };

  // Render
  return (
    <div className={styles.container}>
      {/* Component JSX */}
    </div>
  );
};

// 4. Default export (if needed)
export default MyComponent;
```

### State Management with Zustand

#### Store Definition
```typescript
// store/projectStore.ts
import { create } from 'zustand';
import { devtools } from 'zustand/middleware';

interface Project {
  id: string;
  name: string;
  path: string;
}

interface ProjectStore {
  projects: Project[];
  currentProject: Project | null;
  loading: boolean;

  // Actions
  setProjects: (projects: Project[]) => void;
  addProject: (project: Project) => void;
  removeProject: (id: string) => void;
  setCurrentProject: (project: Project | null) => void;
  loadProjects: () => Promise<void>;
}

export const useProjectStore = create<ProjectStore>()(
  devtools(
    (set, get) => ({
      projects: [],
      currentProject: null,
      loading: false,

      setProjects: (projects) => set({ projects }),

      addProject: (project) =>
        set((state) => ({
          projects: [...state.projects, project],
        })),

      removeProject: (id) =>
        set((state) => ({
          projects: state.projects.filter((p) => p.id !== id),
        })),

      setCurrentProject: (project) => set({ currentProject: project }),

      loadProjects: async () => {
        set({ loading: true });
        try {
          const projects = await ProjectApi.getAll();
          set({ projects, loading: false });
        } catch (error) {
          set({ loading: false });
          throw error;
        }
      },
    }),
    { name: 'ProjectStore' }
  )
);
```

#### Using Store in Components
```typescript
// Component
export const ProjectList: React.FC = () => {
  // Select specific state slices
  const projects = useProjectStore((state) => state.projects);
  const loading = useProjectStore((state) => state.loading);
  const loadProjects = useProjectStore((state) => state.loadProjects);

  useEffect(() => {
    loadProjects();
  }, [loadProjects]);

  if (loading) return <Spin />;

  return (
    <List
      dataSource={projects}
      renderItem={(project) => (
        <List.Item key={project.id}>{project.name}</List.Item>
      )}
    />
  );
};
```

### Routing with React Router

#### Route Configuration
```typescript
// routes.tsx
import { createBrowserRouter } from 'react-router-dom';

import { Layout } from '@/components/layout/Layout';
import { ProjectsPage } from '@/pages/ProjectsPage';
import { EditorPage } from '@/pages/EditorPage';
import { AdminPage } from '@/pages/AdminPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      {
        index: true,
        element: <ProjectsPage />,
      },
      {
        path: 'projects',
        element: <ProjectsPage />,
      },
      {
        path: 'editor/:projectId',
        element: <EditorPage />,
      },
      {
        path: 'admin',
        element: <AdminPage />,
        // Protect admin routes
        loader: async () => {
          const user = await checkAuth();
          if (!user.isAdmin) {
            throw redirect('/');
          }
          return user;
        },
      },
    ],
  },
]);
```

#### Navigation
```typescript
import { useNavigate, useParams } from 'react-router-dom';

export const ProjectCard: React.FC<ProjectCardProps> = ({ project }) => {
  const navigate = useNavigate();

  const handleOpen = () => {
    navigate(`/editor/${project.id}`);
  };

  return <Button onClick={handleOpen}>Open</Button>;
};

// Access route params
export const EditorPage: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();

  return <div>Editing project: {projectId}</div>;
};
```

### API Client

#### API Module Structure
```typescript
// api/client.ts
import axios from 'axios';

export const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add interceptors
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Handle unauthorized
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

#### API Service
```typescript
// api/projectApi.ts
import { apiClient } from './client';

export interface Project {
  id: string;
  name: string;
  path: string;
}

export const ProjectApi = {
  getAll: async (): Promise<Project[]> => {
    const response = await apiClient.get<Project[]>('/projects');
    return response.data;
  },

  getById: async (id: string): Promise<Project> => {
    const response = await apiClient.get<Project>(`/projects/${id}`);
    return response.data;
  },

  create: async (project: Omit<Project, 'id'>): Promise<Project> => {
    const response = await apiClient.post<Project>('/projects', project);
    return response.data;
  },

  update: async (id: string, project: Partial<Project>): Promise<Project> => {
    const response = await apiClient.put<Project>(`/projects/${id}`, project);
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/projects/${id}`);
  },
};
```

### Custom Hooks

#### Data Fetching Hook
```typescript
// hooks/useProjects.ts
import { useState, useEffect } from 'react';
import { ProjectApi, Project } from '@/api/projectApi';

export function useProjects() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadProjects();
  }, []);

  const loadProjects = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await ProjectApi.getAll();
      setProjects(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load projects');
    } finally {
      setLoading(false);
    }
  };

  const createProject = async (project: Omit<Project, 'id'>) => {
    const newProject = await ProjectApi.create(project);
    setProjects([...projects, newProject]);
    return newProject;
  };

  return {
    projects,
    loading,
    error,
    reload: loadProjects,
    createProject,
  };
}

// Usage
const { projects, loading, error, reload } = useProjects();
```

### Internationalization (i18n)

#### Configuration
```typescript
// i18n/config.ts
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import enTranslations from './locales/en.json';
import ruTranslations from './locales/ru.json';

i18n.use(initReactI18next).init({
  resources: {
    en: { translation: enTranslations },
    ru: { translation: ruTranslations },
  },
  lng: localStorage.getItem('language') || 'en',
  fallbackLng: 'en',
  interpolation: {
    escapeValue: false,
  },
});

export default i18n;
```

#### Translation Files
```json
// i18n/locales/en.json
{
  "projects": {
    "title": "Projects",
    "create": "Create Project",
    "open": "Open",
    "delete": "Delete",
    "confirmDelete": "Are you sure you want to delete {{name}}?"
  },
  "common": {
    "save": "Save",
    "cancel": "Cancel",
    "loading": "Loading..."
  }
}
```

#### Usage in Components
```typescript
import { useTranslation } from 'react-i18next';

export const ProjectList: React.FC = () => {
  const { t } = useTranslation();

  return (
    <div>
      <h1>{t('projects.title')}</h1>
      <Button>{t('projects.create')}</Button>
    </div>
  );
};

// With interpolation
const confirmDelete = (name: string) => {
  Modal.confirm({
    title: t('projects.confirmDelete', { name }),
    onOk: handleDelete,
  });
};
```

### Styling with SCSS Modules

#### Component Styles
```scss
// MyComponent.module.scss
.container {
  padding: 20px;
  background-color: #fff;

  .header {
    font-size: 24px;
    font-weight: bold;
    margin-bottom: 16px;
  }

  .content {
    display: flex;
    gap: 16px;

    .card {
      flex: 1;
      padding: 16px;
      border: 1px solid #d9d9d9;
      border-radius: 4px;

      &:hover {
        border-color: #1890ff;
      }
    }
  }
}
```

#### Usage
```typescript
import styles from './MyComponent.module.scss';

export const MyComponent: React.FC = () => {
  return (
    <div className={styles.container}>
      <h1 className={styles.header}>Title</h1>
      <div className={styles.content}>
        <div className={styles.card}>Card 1</div>
        <div className={styles.card}>Card 2</div>
      </div>
    </div>
  );
};
```

---

## Testing

### Unit Tests with Jest & React Testing Library

```typescript
// __tests__/ProjectCard.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { ProjectCard } from '../ProjectCard';

const mockProject = {
  id: '1',
  name: 'Test Project',
  path: '/path/to/project',
};

describe('ProjectCard', () => {
  it('renders project name', () => {
    render(<ProjectCard project={mockProject} onOpen={jest.fn()} />);
    expect(screen.getByText('Test Project')).toBeInTheDocument();
  });

  it('calls onOpen when button clicked', () => {
    const handleOpen = jest.fn();
    render(<ProjectCard project={mockProject} onOpen={handleOpen} />);

    fireEvent.click(screen.getByText('Open'));
    expect(handleOpen).toHaveBeenCalledWith(mockProject);
  });
});
```

---

## Build & Development

### Development Server
```bash
npm install
npm start
# Opens http://localhost:3000
```

### Production Build
```bash
npm run build
# Output: dist/
```

### Maven Integration
```bash
mvn clean install
# Runs npm install and npm run build automatically
```

---

## Best Practices

### Performance
- ✅ Use React.memo() for expensive components
- ✅ Lazy load routes with React.lazy()
- ✅ Virtualize long lists
- ✅ Debounce expensive operations

### Code Quality
- ✅ Enable TypeScript strict mode
- ✅ Use ESLint and Prettier
- ✅ Write tests for critical components
- ✅ Keep components small and focused

### Accessibility
- ✅ Use semantic HTML
- ✅ Add ARIA labels where needed
- ✅ Test keyboard navigation
- ✅ Support screen readers

---

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT
**Status**: Active Development
