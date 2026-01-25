import { useNavigate } from 'react-router-dom';
import FlowHeader from '../../ui/FlowHeader';
import { LOST_CATEGORIES } from '../../core/categories';

export default function LostCategoryPage() {
  const navigate = useNavigate();

  return (
    <main className="flow-page">
      <div className="flow-inner">
        <FlowHeader breadcrumb="Step 1 of 2" title="I Lost an Item" />

        <p className="flow-breadcrumb">What kind of item did you lose?</p>

        <div className="flow-grid">
          {LOST_CATEGORIES.map((category) => (
            <button
              key={category}
              type="button"
              className="flow-grid-option"
              onClick={() => navigate(`/lost/where?category=${encodeURIComponent(category)}`)}
            >
              {category}
            </button>
          ))}
        </div>
      </div>
    </main>
  );
}
