import React from 'react';
import Tesseract from './components/Tesseract';

// Main App component to render the Tesseract
export default function App() {
  return (
      <Tesseract size={400} speed={0.0015} />
  );
}
