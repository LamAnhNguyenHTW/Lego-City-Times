'use client';

import React from 'react';

interface ErrorBannerProps {
  title?: string;
  message: string;
  onRetry?: () => void;
  variant?: 'inline' | 'full';
}

export default function ErrorBanner({
  title = 'Etwas ist schiefgelaufen',
  message,
  onRetry,
  variant = 'inline',
}: ErrorBannerProps) {
  const baseClasses =
    variant === 'full'
      ? 'lego-brick bg-white p-8 text-center my-8'
      : 'border-3 border-lego-red bg-lego-red/10 p-4 rounded my-4';

  return (
    <div className={baseClasses} role="alert" aria-live="assertive">
      <h2 className="text-lg font-black text-lego-red mb-2 uppercase">⚠ {title}</h2>
      <p className="font-semibold text-lego-gray-dark mb-3 text-sm">{message}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="lego-btn lego-btn-yellow text-xs py-1 px-4"
          type="button"
        >
          Erneut versuchen
        </button>
      )}
    </div>
  );
}
