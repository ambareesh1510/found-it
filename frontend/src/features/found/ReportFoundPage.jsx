import { useNavigate } from 'react-router-dom';
import ReportForm from '../reports/ReportForm';
import { POST_TYPES } from '../../core/reports';

/** "Report found" (green banner per ui-spec: a photo is needed so the owner can recognise the item). */
export default function ReportFoundPage() {
  const navigate = useNavigate();

  return (
    <main className="flow-page">
      <div className="flow-inner">
        {/* Under the Home tab, so the tab bar handles navigation: no back button or breadcrumb. */}
        <header className="flow-header">
          <h1 className="flow-title">I Found an Item</h1>
        </header>
        <ReportForm
          postType={POST_TYPES.FOUND}
          banner="Thanks for reporting. A clear photo helps the owner recognize the item."
          onSubmitted={() =>
            navigate('/', { replace: true, state: { notice: 'Your found item report is posted.' } })
          }
        />
      </div>
    </main>
  );
}
