import { useLocation, useNavigate } from 'react-router-dom';
import FlowHeader from '../../ui/FlowHeader';
import ReportForm from '../reports/ReportForm';
import { POST_TYPES } from '../../core/reports';

/** "Report lost" after a search with no match. Prefilled from the search declaration in router state. */
export default function ReportLostPage() {
  const navigate = useNavigate();
  const { state } = useLocation();

  return (
    <main className="flow-page">
      <div className="flow-inner">
        <FlowHeader breadcrumb="Report a lost item" title="I Lost an Item" />
        <ReportForm
          postType={POST_TYPES.LOST}
          prefill={state?.prefill ?? null}
          onSubmitted={() =>
            navigate('/', { replace: true, state: { notice: 'Your lost item report is posted.' } })
          }
        />
      </div>
    </main>
  );
}
