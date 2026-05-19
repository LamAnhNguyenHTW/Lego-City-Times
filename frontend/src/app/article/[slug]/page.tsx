'use client';

import React, { useEffect, useState, use } from 'react';
import Link from 'next/link';

interface Category {
  id: number;
  name: string;
  slug: string;
}

interface Tag {
  id: number;
  name: string;
  slug: string;
}

interface Article {
  id: number;
  title: string;
  slug: string;
  subtitle: string;
  content: string;
  author: string;
  category?: Category;
  tags?: Tag[];
  status: string;
  coverImageUrl?: string;
  viewCount: number;
  createdAt: string;
  publishedAt?: string;
}

interface PageProps {
  params: Promise<{ slug: string }>;
}

export default function ArticleDetailPage({ params }: PageProps) {
  const { slug } = use(params);
  const [article, setArticle] = useState<Article | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!slug) return;
    let ignore = false;

    async function fetchArticle() {
      try {
        const res = await fetch(`/api/v1/articles/slug/${slug}`);
        if (res.ok) {
          const data = await res.json();
          if (!ignore) setArticle(data);
        } else if (!ignore) {
          setError('Artikel wurde nicht gefunden.');
        }
      } catch (err) {
        console.error(err);
        if (!ignore) setError('Verbindung zum Server fehlgeschlagen.');
      } finally {
        if (!ignore) setLoading(false);
      }
    }

    fetchArticle();
    return () => {
      ignore = true;
    };
  }, [slug]);

  const renderLegoPlaceholder = (colorClass = 'color-nachrichten') => (
    <div className={`w-full h-full flex items-center justify-center ${colorClass}`} style={{ minHeight: '300px', position: 'relative', overflow: 'hidden' }}>
      <div className="lego-studs absolute inset-0 opacity-20" />
      <svg width="120" height="80" viewBox="0 0 120 80" fill="none" xmlns="http://www.w3.org/2000/svg" className="drop-shadow-md z-10 scale-125">
        <rect width="120" height="80" fill="#E60012" rx="6" stroke="#1D1D1D" strokeWidth="4"/>
        <circle cx="30" cy="22" r="14" fill="#E60012" stroke="#1D1D1D" strokeWidth="3"/>
        <circle cx="90" cy="22" r="14" fill="#E60012" stroke="#1D1D1D" strokeWidth="3"/>
        <circle cx="30" cy="58" r="14" fill="#E60012" stroke="#1D1D1D" strokeWidth="3"/>
        <circle cx="90" cy="58" r="14" fill="#E60012" stroke="#1D1D1D" strokeWidth="3"/>
      </svg>
    </div>
  );

  return (
    <div className="pb-20">
      
      {/* ── BANNER HEADER ────────────────────────────────────────────────────────── */}
      <header className="lego-studs lego-studs-blue border-b-8 border-lego-black py-4 text-white relative shadow-lg">
        <div className="lego-container flex items-center justify-between">
          <Link href="/" className="flex items-center gap-3 group cursor-pointer">
            <div className="bg-lego-red border-3 border-lego-black p-2 rounded transform rotate-[-2deg] group-hover:rotate-[0deg] transition-transform duration-200">
              <span className="font-display font-black text-xl uppercase text-white">LEGO</span>
            </div>
            <div className="flex flex-col text-left">
              <span className="font-display font-black text-lg text-lego-yellow uppercase drop-shadow-[1px_1px_0px_#1D1D1D]">CITY NEWS</span>
            </div>
          </Link>
          <Link href="/" className="lego-btn lego-btn-yellow py-1 px-4 text-xs">
            Zurück zur Übersicht
          </Link>
        </div>
      </header>

      {/* ── CONTENT ─────────────────────────────────────────────────────────────── */}
      <main className="lego-container mt-10 max-w-4xl">
        {loading ? (
          <div className="text-center font-bold text-2xl py-20 text-lego-black">
            <div className="inline-block animate-bounce text-lego-yellow text-4xl mr-2">■</div>
            Artikel wird aufgebaut...
          </div>
        ) : error || !article ? (
          <div className="lego-brick bg-white p-8 text-center">
            <h2 className="text-2xl font-black text-lego-red mb-4">FEHLER</h2>
            <p className="font-bold text-lego-gray-dark mb-6">{error || 'Artikel nicht gefunden.'}</p>
            <Link href="/" className="lego-btn lego-btn-blue text-sm">
              Zurück zur Startseite
            </Link>
          </div>
        ) : (
          <div className="flex flex-col">
            
            {/* Header brick color */}
            <div className={`lego-brick-header ${article.category?.slug ? `color-${article.category.slug.toLowerCase()}` : 'color-nachrichten'} flex justify-between items-center`}>
              <span className="font-black text-sm uppercase">
                {article.category?.name || 'ALLGEMEIN'}
              </span>
              <span className="text-xs font-semibold">
                👁 {article.viewCount} Aufrufe
              </span>
            </div>

            {/* Main content body */}
            <article className="lego-brick bg-white p-6 md:p-10">
              
              {/* Cover image */}
              <div className="border-4 border-lego-black rounded-lg overflow-hidden mb-8 shadow">
                {article.coverImageUrl ? (
                  <img src={article.coverImageUrl} alt={article.title} className="w-full h-auto max-h-[450px] object-cover" />
                ) : (
                  renderLegoPlaceholder(article.category?.slug ? `color-${article.category.slug.toLowerCase()}` : 'color-nachrichten')
                )}
              </div>

              {/* Title & subtitle */}
              <h1 className="text-3xl md:text-5xl font-black text-lego-black leading-tight mb-3">
                {article.title}
              </h1>
              <p className="text-lg md:text-xl text-lego-gray-dark font-bold mb-6 border-l-4 border-lego-yellow pl-4 italic">
                {article.subtitle}
              </p>

              {/* Metadata */}
              <div className="flex flex-wrap items-center gap-x-6 gap-y-3 text-xs font-bold text-lego-gray-dark border-y-2 border-lego-gray-light py-4 mb-8">
                <span>Reporter: <span className="underline decoration-lego-yellow decoration-2">{article.author}</span></span>
                <span className="hidden md:inline text-lego-gray-base">•</span>
                <span>Veröffentlicht am: {article.publishedAt ? new Date(article.publishedAt).toLocaleString('de-DE') : 'DRAFT'}</span>
                {article.tags && article.tags.length > 0 && (
                  <>
                    <span className="hidden md:inline text-lego-gray-base">•</span>
                    <div className="flex flex-wrap gap-2">
                      {article.tags.map(t => (
                        <span key={t.id} className="inline-flex items-center bg-lego-gray-light border border-lego-black/30 px-2.5 py-1 rounded text-[10px] leading-none uppercase tracking-wider">
                          #{t.name}
                        </span>
                      ))}
                    </div>
                  </>
                )}
              </div>

              {/* Article text */}
              <div className="prose max-w-none text-lego-black text-base md:text-lg leading-relaxed font-normal whitespace-pre-wrap">
                {article.content}
              </div>

              {/* Footer navigation */}
              <div className="mt-12 pt-6 border-t-2 border-lego-gray-light flex flex-col sm:flex-row justify-between items-center gap-4">
                <Link href="/" className="lego-btn lego-btn-blue text-sm w-full sm:w-auto justify-center">
                  ← Zurück zur Startseite
                </Link>
                
                {/* Custom fun social reaction buttons */}
                <div className="flex gap-2">
                  <button className="lego-btn lego-btn-yellow text-xs py-1 px-3" onClick={() => alert('Gefällt mir! Danke für die Unterstützung!')}>
                    👍 Cool (+1)
                  </button>
                  <button className="lego-btn lego-btn-red text-xs py-1 px-3" onClick={() => alert('Artikel geteilt!')}>
                    🔗 Teilen
                  </button>
                </div>
              </div>

            </article>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="mt-20 border-t-4 border-lego-black bg-lego-black py-8 text-center text-lego-white/60 text-xs">
        <div className="lego-container">
          <p className="font-bold mb-2">LEGO CITY TIMES NEWS PORTAL</p>
          <p>© 2026 LEGO City Media Group. All Rights Reserved.</p>
        </div>
      </footer>

    </div>
  );
}
