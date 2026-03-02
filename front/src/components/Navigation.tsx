import { NavLink } from 'react-router-dom';
import { clsx } from 'clsx';

export function Navigation() {
  return (
    <nav className="bg-black text-white">
      <div className="max-w-4xl mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-white rounded-lg flex items-center justify-center">
              <span className="text-black font-bold text-lg">L</span>
            </div>
            <span className="font-bold text-xl tracking-tight">Libera.ai</span>
          </div>

          <div className="flex items-center gap-1">
            <NavLink
              to="/"
              className={({ isActive }) =>
                clsx(
                  'px-4 py-2 rounded-lg text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-white text-black'
                    : 'text-gray-300 hover:text-white hover:bg-gray-800'
                )
              }
            >
              Pagar
            </NavLink>
            <NavLink
              to="/exit"
              className={({ isActive }) =>
                clsx(
                  'px-4 py-2 rounded-lg text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-white text-black'
                    : 'text-gray-300 hover:text-white hover:bg-gray-800'
                )
              }
            >
              Liberar Saída
            </NavLink>
          </div>
        </div>
      </div>
    </nav>
  );
}
