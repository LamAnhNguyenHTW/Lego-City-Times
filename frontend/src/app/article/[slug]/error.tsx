'use client';

import React, { useEffect } from 'react';
import Link from 'next/link';
import ErrorBanner from '../../../components/ErrorBanner';

export default function ArticleError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error('Fehler im Artikel-Detail:', error);
  }, [error]);

  return (
    <div className="lego-container py-12">
      <ErrorBanner
        variant="full"
        title="Artikel kann nicht geladen werden"
        message="Der Inhalt ist gerade nicht verfügbar. Möglicherweise ist der Backend-Service oder die Datenbank ausgefallen."
        onRetry={reset}
      />
      <div className="text-center mt-6">
        <Link href="/" className="lego-btn lego-btn-blue text-sm">
          Zurück zur Startseite
        </Link>
      </div>
    </div>
  );
}
