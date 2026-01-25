/**
 * Top block used by the lost flow screens: optional breadcrumb (e.g. "Step 1 of 2") and title.
 * There is no back button: the tab bar handles navigation.
 */
export default function FlowHeader({ breadcrumb, title }) {
  return (
    <header className="flow-header">
      <div>
        {breadcrumb && <p className="flow-breadcrumb">{breadcrumb}</p>}
        <h1 className="flow-title">{title}</h1>
      </div>
    </header>
  );
}
