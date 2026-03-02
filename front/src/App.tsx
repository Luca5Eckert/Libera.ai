import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Navigation } from './components/Navigation';
import { PaymentPage } from './pages/PaymentPage';
import { ExitPage } from './pages/ExitPage';

function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen bg-gray-50">
        <Navigation />
        <Routes>
          <Route path="/" element={<PaymentPage />} />
          <Route path="/exit" element={<ExitPage />} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}

export default App;
