import { Link } from 'react-router-dom';
import './legal.css';

// Replace with the email people should use for privacy questions before you publish this page.
const CONTACT_EMAIL = 'founditsupportemail@gmail.com';
const LAST_UPDATED = 'November 11, 2026';

function PrivacyPolicy() {
  return (
    <div className="legal-page">
      <main className="legal-card">
        <p className="legal-back">
          <Link to="/login">Back to login</Link>
        </p>
        <h1 className="legal-title">Privacy Policy</h1>
        <p className="legal-updated">Last updated: {LAST_UPDATED}</p>

        <p>
          This policy explains what the Found it app collects, how we use it, and the choices you have.
          Found it is a lost-and-found app for the Georgia Tech community.
        </p>

        <h2>1. Information you give us</h2>
        <ul>
          <li>Account details: your username, email address, and password. Passwords are stored only as secure hashes, never as plain text.</li>
          <li>Sign-in details: if you sign in with Google or Apple, we receive an identifier from that provider and the email on that account.</li>
          <li>Posts and claims: the titles, descriptions, categories, locations, and photos you add to lost or found items, and any claims you make.</li>
          <li>Messages: the chat messages you send about an item.</li>
          <li>Search details: the location, category, search radius, and time window you choose when you search for lost items.</li>
        </ul>

        <h2>2. Location</h2>
        <p>
          When you use the map or search with your location, the app reads your device location.
        </p>
        <p>
          The app can also save a location sample about every five minutes while it is open, for the Timeline search.
          These samples are kept on your device for up to seven days. When you run a Timeline search, the samples
          are sent with that search. We do not keep them as a location history.
        </p>

        <h2>3. Photos</h2>
        <p>
          Photos you add to a post are stored with that post. They are visible to other users who can see the post.
        </p>

        <h2>4. How we use information</h2>
        <p>
          We use your information to create and run your account, show lost and found posts, run searches, let
          people chat and make claims, send verification and password reset emails, and keep the service secure
          and working.
        </p>

        <h2>5. Who we share it with</h2>
        <p>We do not sell your personal information. We share it only with the services that run Found it:</p>
        <ul>
          <li>Render, which hosts our backend and this website.</li>
          <li>Neon, which stores our database.</li>
          <li>Brevo, which sends account emails such as verification and password reset.</li>
          <li>Google, for sign-in (if you use it) and for the map.</li>
          <li>Apple, for sign-in (if you use it).</li>
        </ul>
        <p>
          Other users can see the details you post on an item, along with your username.
        </p>

        <h2>6. How long we keep information</h2>
        <p>
          We keep your information while your account exists. When you delete your account, we delete your account
          information and the data tied to it.
        </p>

        <h2>7. Your choices</h2>
        <ul>
          <li>Edit or delete your own posts.</li>
          <li>Delete your account from the app.</li>
          <li>Contact us to ask for a copy of your information or to ask for it to be deleted.</li>
        </ul>

        <h2>8. Security</h2>
        <p>
          Our apps send data over encrypted connections (HTTPS), and passwords are stored as hashes. No service is
          perfectly secure, so please use a strong, unique password.
        </p>

        <h2>9. Children</h2>
        <p>
          Found it is not intended for children under 13, and we do not knowingly collect information from them.
        </p>

        <h2>10. Changes to this policy</h2>
        <p>
          We may update this policy. When we do, we will change the date at the top of this page.
        </p>

        <h2>11. Contact</h2>
        <p>
          For privacy questions, email <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>.
        </p>
      </main>
    </div>
  );
}

export default PrivacyPolicy;
