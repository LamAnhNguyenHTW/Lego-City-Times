'use client';

import React, { useEffect } from 'react';
import ErrorBanner from '../components/ErrorBanner';

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error('Unbehandelter Fehler im Frontend:', error);
  }, [error]);

  return (
    <div className="lego-container py-12">
      <ErrorBanner
        variant="full"
        title="Die Seite konnte nicht geladen werden"
        message="Wir konnten die Inhalte gerade nicht abrufen. Der Backend-Dienst oder die Datenbank ist möglicherweise nicht erreichbar."
        onRetry={reset}
      />
    </div>
  );
}
