import { useLocation, useNavigate } from 'react-router-dom';
import { TABS } from '../core/navigation.js';
import { IconHome, IconMap, IconMessage, IconUser } from './icons';
import './tabBar.css';

const ICONS = {
  home: IconHome,
  map: IconMap,
  messages: IconMessage,
  profile: IconUser,
};

/** Bottom tab switcher for the signed-in app. The active tab is passed in, so the routing logic stays in core. */
export default function AppTabBar({ activeTabId }) {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const currentPath = pathname.replace(/\/+$/, '') || '/';

  return (
    <nav className="app-tab-bar" aria-label="Main">
      {TABS.map((tab) => {
        const Icon = ICONS[tab.id];
        const isActive = tab.id === activeTabId;
        return (
          <button
            key={tab.id}
            type="button"
            className="app-tab"
            aria-current={isActive ? 'page' : undefined}
            onClick={() => {
              // Tapping the tab you are on does nothing, but a flow screen (e.g. /lost/where) also counts as
              // its tab, so Home must still navigate back to the root page.
              if (currentPath !== tab.path) navigate(tab.path);
            }}
          >
            <Icon />
            <span>{tab.label}</span>
          </button>
        );
      })}
    </nav>
  );
}
