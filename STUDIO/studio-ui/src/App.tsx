import React from 'react';
import Tesseract from './components/Tesseract';

// Main App component to render the Tesseract
export default function App() {
  return (
      <div 
        style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
          width: '100vw',
          backgroundColor: 'white'
        }}
      >
        <Tesseract size={350} speed={0.0015} />
      </div>
  );
}
