'use client';

import React, { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { apiFetch, describeApiError } from '../lib/api';
import ErrorBanner from '../components/ErrorBanner';

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
  author: string;
  category?: Category;
  tags?: Tag[];
  status: string;
  coverImageUrl?: string;
  viewCount: number;
  createdAt: string;
  publishedAt?: string;
}

interface UserSession {
  token: string;
  username: string;
  role: string;
}

const mainNav = [
  { label: 'News', slug: null },
  { label: 'Stadt', slug: 'news' },
  { label: 'Polizei', slug: 'polizei' },
  { label: 'Politik', slug: 'politik' },
  { label: 'Wirtschaft', slug: 'wirtschaft' },
  { label: 'Sport', slug: 'sport' },
  { label: 'Kultur', slug: 'kultur' },
];

const panelCategories = [
  { title: 'Polizei', slug: 'polizei', className: 'lego-btn-blue' },
  { title: 'Politik', slug: 'politik', className: 'lego-btn-red' },
  { title: 'Wirtschaft', slug: 'wirtschaft', className: 'lego-btn-green' },
  { title: 'Sport', slug: 'sport', className: 'lego-btn-orange' },
];

function loadSession(): UserSession | null {
  if (typeof window === 'undefined') return null;
  const token = localStorage.getItem('token');
  const username = localStorage.getItem('username');
  const role = localStorage.getItem('role');
  return token && username && role ? { token, username, role } : null;
}

function ArticleImage({ article, variant = 'card' }: { article?: Article; variant?: 'hero' | 'card' | 'thumb' }) {
  const sizeClass = variant === 'hero' ? 'min-h-[360px]' : variant === 'thumb' ? 'h-24' : 'h-44';
  const colorClass = article?.category?.slug ? `color-${article.category.slug}` : 'color-news';

  if (article?.coverImageUrl) {
    return <img src={article.coverImageUrl} alt={article.title} className={`w-full ${sizeClass} object-cover`} />;
  }

  return (
    <div className={`lego-scene ${colorClass} ${sizeClass}`}>
      <div className="scene-skyline" />
      <div className="scene-road" />
      <div className="scene-minifig" />
    </div>
  );
}

export default function HomePage() {
  const [articles, setArticles] = useState<Article[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<Article[] | null>(null);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [usernameInput, setUsernameInput] = useState('');
  const [passwordInput, setPasswordInput] = useState('');
  const [authError, setAuthError] = useState('');
  const [userSession, setUserSession] = useState<UserSession | null>(() => loadSession());
  const [currentSlide, setCurrentSlide] = useState(0);

  useEffect(() => {
    let ignore = false;

    async function load() {
      setLoading(true);
      setLoadError(null);
      try {
        const res = await apiFetch('/api/v1/articles?status=PUBLISHED&size=50');
        const data = await res.json();
        if (!ignore) setArticles(data.content || []);
      } catch (error) {
        console.error('Fehler beim Laden der Startseite:', error);
        if (!ignore) {
          setArticles([]);
          setLoadError(describeApiError(error));
        }
      } finally {
        if (!ignore) setLoading(false);
      }
    }

    load();
    return () => {
      ignore = true;
    };
  }, [reloadKey]);

  const handleSearchSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    const query = searchQuery.trim();
    if (!query) {
      setSearchResults(null);
      return;
    }

    setSearching(true);
    setSearchError(null);
    try {
      const res = await apiFetch(`/api/v1/search/articles?q=${encodeURIComponent(query)}`);
      const data = await res.json();
      setSearchResults(data.content || []);
    } catch (error) {
      console.warn('Search-Service nicht erreichbar, versuche Fallback:', error);
      try {
        const fallbackRes = await apiFetch(`/api/v1/articles?status=PUBLISHED&search=${encodeURIComponent(query)}`);
        const data = await fallbackRes.json();
        setSearchResults(data.content || []);
      } catch (fallbackError) {
        console.error('Suche komplett fehlgeschlagen:', fallbackError);
        setSearchResults([]);
        setSearchError(describeApiError(fallbackError));
      }
    } finally {
      setSearching(false);
    }
  };

  const handleLoginSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setAuthError('');

    try {
      const res = await fetch('/api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: usernameInput, password: passwordInput }),
      });

      if (!res.ok) {
        setAuthError('Falsche Anmeldedaten.');
        return;
      }

      const data = await res.json();
      localStorage.setItem('token', data.token);
      localStorage.setItem('username', data.username);
      localStorage.setItem('role', data.role);
      setUserSession({ token: data.token, username: data.username, role: data.role });
      setShowLoginModal(false);
      setUsernameInput('');
      setPasswordInput('');
    } catch (error) {
      console.error('Login fehlgeschlagen:', error);
      setAuthError('Verbindung zum Server fehlgeschlagen.');
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    setUserSession(null);
  };

  const filteredArticles = useMemo(() => {
    if (!selectedCategory) return articles;
    return articles.filter((article) => article.category?.slug === selectedCategory);
  }, [articles, selectedCategory]);

  const slideshowArticles = useMemo(() => {
    const breaking = filteredArticles.filter((article) =>
      article.tags?.some((tag) => tag.slug === 'breaking' || tag.name.toLowerCase() === 'breaking')
    );
    const pool = breaking.length > 0 ? breaking : filteredArticles;
    return pool.slice(0, 4);
  }, [filteredArticles]);

  useEffect(() => {
    setCurrentSlide(0);
  }, [slideshowArticles]);

  useEffect(() => {
    if (slideshowArticles.length <= 1) return;
    const timer = setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % slideshowArticles.length);
    }, 5000);
    return () => clearInterval(timer);
  }, [slideshowArticles]);

  const topNews = useMemo(() => {
    const slideshowIds = new Set(slideshowArticles.map((a) => a.id));
    let filtered = filteredArticles.filter((article) => !slideshowIds.has(article.id));
    if (filtered.length === 0) {
      const activeArticle = slideshowArticles[currentSlide];
      filtered = filteredArticles.filter((article) => article.id !== activeArticle?.id);
    }
    return filtered.slice(0, 3);
  }, [filteredArticles, slideshowArticles, currentSlide]);

  const visibleArticles = searchResults ?? filteredArticles;

  const getCategoryArticles = (slug: string) =>
    articles.filter((article) => article.category?.slug === slug).slice(0, 3);

  return (
    <div className="site-shell">
      <header className="news-hero">
        <div className="lego-container header-grid">
          <Link href="/" className="brand-lockup" onClick={() => setSelectedCategory(null)}>
            <span className="brand-lego">LEGO</span>
            <span className="brand-city">CITY</span>
            <span className="brand-news">NEWS</span>
          </Link>

          <form onSubmit={handleSearchSubmit} className="top-search">
            <input
              type="text"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="Suche"
            />
            <button type="submit">Suche</button>
          </form>

          <div className="user-actions">
            {userSession ? (
              <>
                {userSession.role === 'ADMIN' && (
                  <Link href="/admin" className="nav-action">Admin</Link>
                )}
                <button onClick={handleLogout} className="nav-action">Abmelden</button>
              </>
            ) : (
              <button onClick={() => setShowLoginModal(true)} className="nav-action">Anmelden</button>
            )}
          </div>
        </div>
      </header>

      <nav className="main-nav">
        <div className="lego-container nav-row">
          <button
            onClick={() => {
              setSelectedCategory(null);
              setSearchResults(null);
            }}
            className="home-tab"
            aria-label="Startseite"
          >
            Haus
          </button>
          {mainNav.map((item) => (
            <button
              key={item.label}
              onClick={() => {
                setSelectedCategory(item.slug);
                setSearchResults(null);
              }}
              className={selectedCategory === item.slug && searchResults === null ? 'active' : ''}
            >
              {item.label}
            </button>
          ))}
        </div>
      </nav>

      <main className="lego-container news-layout">
        {loadError ? (
          <ErrorBanner
            variant="full"
            title="Nachrichten konnten nicht geladen werden"
            message={loadError}
            onRetry={() => setReloadKey((k) => k + 1)}
          />
        ) : loading ? (
          <div className="empty-state">Nachrichten werden geladen...</div>
        ) : searchResults !== null ? (
          <section className="search-results">
            <div className="section-title">
              <h1>Suchergebnisse fur &quot;{searchQuery}&quot;</h1>
              <button onClick={() => setSearchResults(null)}>Zuruck</button>
            </div>
            {searching ? (
              <div className="empty-state">Suche lauft...</div>
            ) : searchError ? (
              <ErrorBanner
                title="Suche nicht verfügbar"
                message={searchError}
                onRetry={() => handleSearchSubmit({ preventDefault: () => {} } as React.FormEvent)}
              />
            ) : visibleArticles.length === 0 ? (
              <div className="empty-state">Keine Artikel gefunden.</div>
            ) : (
              <div className="article-grid">
                {visibleArticles.map((article) => (
                  <ArticleCard key={article.id} article={article} />
                ))}
              </div>
            )}
          </section>
        ) : filteredArticles.length === 0 ? (
          <div className="empty-state">Keine Artikel in dieser Kategorie vorhanden.</div>
        ) : (
          <>
            <section className="lead-grid">
              {slideshowArticles.length > 0 && (
                (() => {
                  const activeArticle = slideshowArticles[currentSlide] || slideshowArticles[0];
                  const isBreaking = activeArticle.tags?.some((t) => t.slug === 'breaking' || t.name.toLowerCase() === 'breaking');
                  return (
                    <article className="lead-story relative overflow-hidden group">
                      <ArticleImage article={activeArticle} variant="hero" />
                      <div className="breaking-label">
                        {isBreaking ? 'Breaking News' : 'Top Story'}
                      </div>
                      <div className="lead-copy">
                        <h1>{activeArticle.title}</h1>
                        <p>{activeArticle.subtitle}</p>
                        <Link href={`/article/${activeArticle.slug}`}>Mehr lesen</Link>
                      </div>
                      {slideshowArticles.length > 1 && (
                        <div className="slider-dots">
                          {slideshowArticles.map((_, idx) => (
                            <span
                              key={idx}
                              className={currentSlide === idx ? 'active' : ''}
                              onClick={() => setCurrentSlide(idx)}
                            />
                          ))}
                        </div>
                      )}
                    </article>
                  );
                })()
              )}

              <aside className="top-news">
                <h2>Top News</h2>
                {topNews.map((article) => (
                  <Link href={`/article/${article.slug}`} key={article.id} className="top-news-item">
                    <ArticleImage article={article} variant="thumb" />
                    <span className="top-news-copy">
                      <strong>{article.title}</strong>
                      <small>{article.subtitle}</small>
                      <span className="more-link">Mehr lesen &rarr;</span>
                    </span>
                  </Link>
                ))}
                {selectedCategory !== null && (
                  <button
                    onClick={() => {
                      setSelectedCategory(null);
                      setSearchResults(null);
                      setSearchQuery('');
                      window.scrollTo({ top: 0, behavior: 'smooth' });
                    }}
                  >
                    Alle News anzeigen
                  </button>
                )}
              </aside>
            </section>

            <section className="category-panels">
              {panelCategories.map((category) => (
                <div className="category-panel" key={category.slug}>
                  <h2 className={category.className}>{category.title}</h2>
                  <ArticleImage article={getCategoryArticles(category.slug)[0]} />
                  <ul>
                    {getCategoryArticles(category.slug).map((article) => (
                      <li key={article.id}>
                        <Link href={`/article/${article.slug}`}>{article.title}</Link>
                      </li>
                    ))}
                    {getCategoryArticles(category.slug).length === 0 && (
                      <li>Keine aktuellen Meldungen</li>
                    )}
                  </ul>
                  <button onClick={() => setSelectedCategory(category.slug)}>Alle {category.title}-News</button>
                </div>
              ))}
            </section>
          </>
        )}
      </main>

      {showLoginModal && (
        <div className="lego-modal-overlay">
          <div className="lego-modal max-w-sm w-full">
            <div className="lego-brick-header bg-lego-blue flex justify-between items-center">
              <span className="font-black text-white">ANMELDEN</span>
              <button onClick={() => setShowLoginModal(false)} className="text-white hover:text-lego-yellow font-black transition-colors" aria-label="Schließen">X</button>
            </div>
            <form onSubmit={handleLoginSubmit} className="p-6 flex flex-col gap-4">
              {authError && (
                <div className="bg-lego-red/10 border-2 border-lego-red p-3 rounded text-lego-red text-xs font-bold">
                  {authError}
                </div>
              )}
              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold uppercase text-lego-gray-dark">Benutzername</label>
                <input
                  type="text"
                  required
                  placeholder="Benutzername"
                  value={usernameInput}
                  onChange={(event) => setUsernameInput(event.target.value)}
                  className="lego-input text-sm"
                />
              </div>
              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold uppercase text-lego-gray-dark">Passwort</label>
                <input
                  type="password"
                  required
                  placeholder="Passwort"
                  value={passwordInput}
                  onChange={(event) => setPasswordInput(event.target.value)}
                  className="lego-input text-sm"
                />
              </div>
              <button type="submit" className="lego-btn lego-btn-blue justify-center mt-2 w-full">
                Anmelden
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

function ArticleCard({ article }: { article: Article }) {
  return (
    <article className="article-card">
      <ArticleImage article={article} />
      <div>
        <span>{article.category?.name || 'News'}</span>
        <h2>{article.title}</h2>
        <p>{article.subtitle}</p>
        <Link href={`/article/${article.slug}`}>Mehr lesen</Link>
      </div>
    </article>
  );
}
