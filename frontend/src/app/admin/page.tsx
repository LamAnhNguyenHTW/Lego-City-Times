'use client';

import React, { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';

interface Category {
  id: number;
  name: string;
  slug: string;
  description?: string;
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

export default function AdminPage() {
  const [token, setToken] = useState(() => typeof window !== 'undefined' ? localStorage.getItem('token') || '' : '');
  const [username, setUsername] = useState(() => typeof window !== 'undefined' ? localStorage.getItem('username') || '' : '');
  const [isAdmin, setIsAdmin] = useState(() => typeof window !== 'undefined' && localStorage.getItem('role') === 'ADMIN' && !!localStorage.getItem('token'));
  
  // Login Form
  const [loginUser, setLoginUser] = useState('');
  const [loginPass, setLoginPass] = useState('');
  const [loginError, setLoginError] = useState('');

  // Dashboard state
  const [activeTab, setActiveTab] = useState<'articles' | 'categories' | 'tags'>('articles');
  const [articles, setArticles] = useState<Article[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(true);

  // Article Form State
  const [showArticleForm, setShowArticleForm] = useState(false);
  const [editingArticleId, setEditingArticleId] = useState<number | null>(null);
  const [artTitle, setArtTitle] = useState('');
  const [artSubtitle, setArtSubtitle] = useState('');
  const [artAuthor, setArtAuthor] = useState('');
  const [artContent, setArtContent] = useState('');
  const [artCategoryId, setArtCategoryId] = useState<string>('');
  const [artTagIds, setArtTagIds] = useState<number[]>([]);
  const [artCoverUrl, setArtCoverUrl] = useState('');
  const [uploadingImage, setUploadingImage] = useState(false);

  // Category Form State
  const [catName, setCatName] = useState('');
  const [catDesc, setCatDesc] = useState('');

  // Tag Form State
  const [tagName, setTagName] = useState('');

  const fetchArticles = useCallback(async () => {
    // Admins want to view ALL articles (Drafts, Published, Archived)
    const res = await fetch('/api/v1/articles?size=100');
    if (res.ok) {
      const data = await res.json();
      setArticles(data.content || []);
    }
  }, []);

  const fetchCategories = useCallback(async () => {
    const res = await fetch('/api/v1/categories');
    if (res.ok) {
      const data = await res.json();
      setCategories(data);
    }
  }, []);

  const fetchTags = useCallback(async () => {
    const res = await fetch('/api/v1/tags');
    if (res.ok) {
      const data = await res.json();
      setTags(data);
    }
  }, []);

  const fetchAllData = useCallback(async () => {
    setLoading(true);
    try {
      await Promise.all([
        fetchArticles(),
        fetchCategories(),
        fetchTags()
      ]);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [fetchArticles, fetchCategories, fetchTags]);

  // Fetch admin list contents once verified
  useEffect(() => {
    if (isAdmin && token) {
      void Promise.resolve().then(fetchAllData);
    }
  }, [fetchAllData, isAdmin, token]);

  // Login handler
  const handleAdminLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError('');
    try {
      const res = await fetch('/api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: loginUser, password: loginPass })
      });

      if (res.ok) {
        const data = await res.json();
        if (data.role === 'ADMIN') {
          localStorage.setItem('token', data.token);
          localStorage.setItem('username', data.username);
          localStorage.setItem('role', data.role);
          
          setToken(data.token);
          setUsername(data.username);
          setIsAdmin(true);
        } else {
          setLoginError('Zugriff verweigert: Nur Administratoren dürfen diesen Bereich betreten.');
        }
      } else {
        setLoginError('Falsche Anmeldedaten.');
      }
    } catch (err) {
      setLoginError('Verbindung zum Server fehlgeschlagen.');
    }
  };

  // Logout handler
  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    setIsAdmin(false);
    setToken('');
    setUsername('');
  };

  // Image Upload handler
  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    setUploadingImage(true);
    const formData = new FormData();
    formData.append('file', files[0]);
    formData.append('altText', 'Artikel Bild');
    
    try {
      const res = await fetch('/api/v1/images/upload', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        },
        body: formData
      });

      if (res.ok) {
        const data = await res.json();
        setArtCoverUrl(data.url);
        alert('Bild erfolgreich hochgeladen!');
      } else {
        alert('Fehler beim Bild-Upload. Bitte Dateigröße und Typ prüfen (nur Bilder erlaubt).');
      }
    } catch (err) {
      alert('Upload fehlgeschlagen.');
    } finally {
      setUploadingImage(false);
    }
  };

  // Open creation form
  const openCreateForm = () => {
    setEditingArticleId(null);
    setArtTitle('');
    setArtSubtitle('');
    setArtAuthor(username);
    setArtContent('');
    setArtCategoryId(categories[0]?.id.toString() || '');
    setArtTagIds([]);
    setArtCoverUrl('');
    setShowArticleForm(true);
  };

  // Open edit form
  const openEditForm = async (art: Article) => {
    try {
      const res = await fetch(`/api/v1/articles/${art.id}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        const fullArt = await res.json();
        setEditingArticleId(fullArt.id);
        setArtTitle(fullArt.title);
        setArtSubtitle(fullArt.subtitle);
        setArtAuthor(fullArt.author);
        setArtContent(fullArt.content);
        setArtCategoryId(fullArt.category?.id?.toString() || '');
        setArtTagIds(fullArt.tags?.map((t: any) => t.id) || []);
        setArtCoverUrl(fullArt.coverImageUrl || '');
        setShowArticleForm(true);
      } else {
        alert('Fehler beim Laden der Artikel-Details.');
      }
    } catch (err) {
      alert('Fehler beim Laden der Artikel-Details.');
    }
  };

  // Save Article (create or update)
  const handleSaveArticle = async (e: React.FormEvent) => {
    e.preventDefault();
    const payload = {
      title: artTitle,
      subtitle: artSubtitle,
      content: artContent,
      author: artAuthor,
      categoryId: artCategoryId ? parseInt(artCategoryId) : null,
      tagIds: artTagIds,
      coverImageUrl: artCoverUrl || null
    };

    const url = editingArticleId ? `/api/v1/articles/${editingArticleId}` : '/api/v1/articles';
    const method = editingArticleId ? 'PUT' : 'POST';

    try {
      const res = await fetch(url, {
        method: method,
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(payload)
      });

      if (res.ok) {
        setShowArticleForm(false);
        fetchAllData();
        alert('Artikel erfolgreich gespeichert!');
      } else {
        alert('Fehler beim Speichern des Artikels. Bitte Pflichtfelder prüfen.');
      }
    } catch (err) {
      alert('Verbindungsfehler.');
    }
  };

  // Delete Article
  const handleDeleteArticle = async (id: number) => {
    if (!confirm('Möchtest du diesen Artikel wirklich endgültig löschen? (Das löscht ihn auch aus Elasticsearch)')) return;

    try {
      const res = await fetch(`/api/v1/articles/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        fetchAllData();
        alert('Artikel gelöscht.');
      }
    } catch (err) {
      alert('Löschen fehlgeschlagen.');
    }
  };

  // Publish Article
  const handlePublishArticle = async (id: number) => {
    try {
      const res = await fetch(`/api/v1/articles/${id}/publish`, {
        method: 'PATCH',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        fetchAllData();
        alert('Artikel wurde veröffentlicht und an Elasticsearch gesendet!');
      } else {
        alert('Fehler beim Veröffentlichen.');
      }
    } catch (err) {
      alert('Fehler.');
    }
  };

  // Archive Article
  const handleArchiveArticle = async (id: number) => {
    try {
      const res = await fetch(`/api/v1/articles/${id}/archive`, {
        method: 'PATCH',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        fetchAllData();
        alert('Artikel wurde archiviert und aus Elasticsearch entfernt!');
      }
    } catch (err) {
      alert('Fehler.');
    }
  };

  // Create Category
  const handleCreateCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch('/api/v1/categories', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ name: catName, description: catDesc })
      });
      if (res.ok) {
        setCatName('');
        setCatDesc('');
        fetchCategories();
        alert('Kategorie erstellt!');
      }
    } catch (err) {
      alert('Fehler beim Erstellen.');
    }
  };

  // Delete Category
  const handleDeleteCategory = async (id: number) => {
    if (!confirm('Kategorie löschen?')) return;
    try {
      const res = await fetch(`/api/v1/categories/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        fetchCategories();
      }
    } catch (err) {
      alert('Fehler beim Löschen.');
    }
  };

  // Create Tag
  const handleCreateTag = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch('/api/v1/tags', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ name: tagName })
      });
      if (res.ok) {
        setTagName('');
        fetchTags();
        alert('Tag erstellt!');
      }
    } catch (err) {
      alert('Fehler.');
    }
  };

  // Delete Tag
  const handleDeleteTag = async (id: number) => {
    if (!confirm('Tag löschen?')) return;
    try {
      const res = await fetch(`/api/v1/tags/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        fetchTags();
      }
    } catch (err) {
      alert('Fehler.');
    }
  };

  // Toggle tag selections in form
  const handleTagToggle = (id: number) => {
    if (artTagIds.includes(id)) {
      setArtTagIds(artTagIds.filter(tid => tid !== id));
    } else {
      setArtTagIds([...artTagIds, id]);
    }
  };

  // Render Login Panel if not Admin
  if (!isAdmin) {
    return (
      <div className="min-h-screen bg-lego-gray-light flex items-center justify-center p-4">
        <div className="lego-modal max-w-md w-full">
          <div className="lego-brick-header bg-lego-red flex justify-between items-center">
            <span className="font-black text-lg">⚠️ ADMIN-BEREICH SCHUTZ</span>
            <Link href="/" className="text-white font-bold hover:underline">Startseite</Link>
          </div>
          <form onSubmit={handleAdminLogin} className="p-6 flex flex-col gap-4">
            <p className="text-sm font-semibold text-lego-gray-dark mb-2">
              Dieser Bereich ist geschützt. Bitte melde dich mit deinen Admin-Zugangsdaten an.
            </p>
            {loginError && (
              <div className="bg-lego-red/10 border-2 border-lego-red p-3 rounded text-lego-red text-xs font-bold">
                {loginError}
              </div>
            )}
            <div className="flex flex-col gap-1">
              <label className="text-xs font-bold uppercase text-lego-gray-dark">Admin-Benutzername</label>
              <input
                type="text"
                required
                value={loginUser}
                onChange={(e) => setLoginUser(e.target.value)}
                className="lego-input font-semibold"
                placeholder="z.B. admin"
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-bold uppercase text-lego-gray-dark">Passwort</label>
              <input
                type="password"
                required
                value={loginPass}
                onChange={(e) => setLoginPass(e.target.value)}
                className="lego-input"
                placeholder="••••••••"
              />
            </div>
            <button type="submit" className="lego-btn lego-btn-red w-full justify-center mt-4">
              Als Admin einloggen
            </button>
          </form>
        </div>
      </div>
    );
  }

  // Render Dashboard
  return (
    <div className="pb-20">
      
      {/* ── HEADER ──────────────────────────────────────────────────────────────── */}
      <header className="lego-studs lego-studs-black border-b-8 border-lego-black py-4 text-white relative shadow-lg">
        <div className="lego-container flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="bg-lego-red border-3 border-lego-black p-2 rounded transform rotate-[-2deg]">
              <span className="font-display font-black text-xl uppercase text-white">LEGO</span>
            </div>
            <span className="font-display font-black text-lg text-lego-yellow uppercase tracking-wide">
              ADMINISTRATOR DASHBOARD
            </span>
          </div>
          <div className="flex gap-2">
            <Link href="/" className="lego-btn lego-btn-blue py-1 px-3 text-xs">
              Zur Startseite
            </Link>
            <button onClick={handleLogout} className="lego-btn lego-btn-red py-1 px-3 text-xs">
              Abmelden ({username})
            </button>
          </div>
        </div>
      </header>

      {/* ── TABS NAVIGATION ────────────────────────────────────────────────────── */}
      <nav className="bg-lego-gray-dark py-3 border-b-4 border-lego-black">
        <div className="lego-container flex gap-2">
          <button
            onClick={() => setActiveTab('articles')}
            className={`lego-btn py-1 px-4 text-xs ${activeTab === 'articles' ? 'lego-btn-yellow' : 'lego-btn-black'}`}
          >
            🧱 Artikel ({articles.length})
          </button>
          <button
            onClick={() => setActiveTab('categories')}
            className={`lego-btn py-1 px-4 text-xs ${activeTab === 'categories' ? 'lego-btn-yellow' : 'lego-btn-black'}`}
          >
            📁 Kategorien ({categories.length})
          </button>
          <button
            onClick={() => setActiveTab('tags')}
            className={`lego-btn py-1 px-4 text-xs ${activeTab === 'tags' ? 'lego-btn-yellow' : 'lego-btn-black'}`}
          >
            🏷️ Tags ({tags.length})
          </button>
        </div>
      </nav>

      {/* ── MAIN CONTENT ────────────────────────────────────────────────────────── */}
      <main className="lego-container mt-8">
        
        {loading ? (
          <div className="text-center font-bold text-xl py-20 text-lego-black">
            Lade Datenbankdaten...
          </div>
        ) : (
          <>
            {/* 1. ARTICLES TAB */}
            {activeTab === 'articles' && (
              <div className="flex flex-col">
                <div className="flex justify-between items-center mb-6">
                  <h2 className="text-2xl font-black text-lego-black">ARTIKEL VERWALTEN</h2>
                  <button onClick={openCreateForm} className="lego-btn lego-btn-green text-sm">
                    ✚ Neuen Artikel schreiben
                  </button>
                </div>

                <div className="lego-brick bg-white overflow-x-auto">
                  <table className="min-w-full text-left border-collapse text-sm">
                    <thead>
                      <tr className="bg-lego-gray-light border-b-3 border-lego-black font-extrabold text-lego-black">
                        <th className="p-3 w-16">ID</th>
                        <th className="p-3">Titel</th>
                        <th className="p-3 w-40">Kategorie</th>
                        <th className="p-3 w-36">Status</th>
                        <th className="p-3 w-24 text-center">Aufrufe</th>
                        <th className="p-3 w-72 text-right">Aktionen</th>
                      </tr>
                    </thead>
                    <tbody>
                      {articles.length === 0 ? (
                        <tr>
                          <td colSpan={6} className="p-8 text-center text-lego-gray-dark font-bold">
                            Keine Artikel vorhanden. Schreib deinen ersten!
                          </td>
                        </tr>
                      ) : (
                        articles.map((art) => (
                          <tr key={art.id} className="border-b border-lego-gray-light hover:bg-lego-gray-light/30">
                            <td className="p-3 font-mono font-bold w-16">#{art.id}</td>
                            <td className="p-3 max-w-xs md:max-w-md">
                              <div className="font-bold text-lego-black break-words">{art.title}</div>
                              <div className="text-xs text-lego-gray-dark">Von {art.author}</div>
                            </td>
                            <td className="p-3 w-40">
                              <span className={`inline-block text-xs font-bold px-2 py-0.5 rounded border border-lego-black/20 color-${art.category?.slug.toLowerCase() || 'nachrichten'} whitespace-nowrap`}>
                                {art.category?.name || 'Keine'}
                              </span>
                            </td>
                            <td className="p-3 w-36">
                              <span className={`inline-block text-[10px] font-black uppercase px-2 py-0.5 rounded whitespace-nowrap ${
                                art.status === 'PUBLISHED' ? 'bg-lego-green/20 text-lego-green border border-lego-green' : 
                                art.status === 'ARCHIVED' ? 'bg-lego-red/20 text-lego-red border border-lego-red' : 
                                'bg-lego-yellow/20 text-lego-orange border border-lego-orange'
                              }`}>
                                {art.status}
                              </span>
                            </td>
                            <td className="p-3 text-center font-bold w-24">{art.viewCount}</td>
                            <td className="p-3 w-72 text-right">
                              <div className="flex justify-end gap-1.5 whitespace-nowrap">
                                {art.status !== 'PUBLISHED' && (
                                  <button onClick={() => handlePublishArticle(art.id)} className="lego-btn lego-btn-green py-0.5 px-2 text-[10px]">
                                    Veröffentlichen
                                  </button>
                                )}
                                {art.status === 'PUBLISHED' && (
                                  <button onClick={() => handleArchiveArticle(art.id)} className="lego-btn lego-btn-orange py-0.5 px-2 text-[10px]">
                                    Archivieren
                                  </button>
                                )}
                                <button onClick={() => openEditForm(art)} className="lego-btn lego-btn-blue py-0.5 px-2 text-[10px]">
                                  Bearbeiten
                                </button>
                                <button onClick={() => handleDeleteArticle(art.id)} className="lego-btn lego-btn-red py-0.5 px-2 text-[10px]">
                                  Löschen
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* 2. CATEGORIES TAB */}
            {activeTab === 'categories' && (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                
                {/* Left: Create form */}
                <div className="flex flex-col">
                  <div className="lego-brick-header bg-lego-blue flex items-center">
                    <span className="font-black text-sm text-white">NEUE KATEGORIE</span>
                  </div>
                  <form onSubmit={handleCreateCategory} className="lego-brick bg-white p-6 flex flex-col gap-5">
                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold uppercase text-lego-gray-dark">Name</label>
                      <input
                        type="text"
                        required
                        value={catName}
                        onChange={(e) => setCatName(e.target.value)}
                        className="lego-input text-sm"
                        placeholder="z.B. Polizei"
                      />
                    </div>
                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold uppercase text-lego-gray-dark">Beschreibung</label>
                      <textarea
                        value={catDesc}
                        onChange={(e) => setCatDesc(e.target.value)}
                        className="lego-input text-sm h-24 resize-none"
                        placeholder="Kurze Beschreibung"
                      />
                    </div>
                    <button type="submit" className="lego-btn lego-btn-blue w-full justify-center text-sm py-2.5 mt-2">
                      Speichern
                    </button>
                  </form>
                </div>

                {/* Right: Table list */}
                <div className="md:col-span-2 flex flex-col">
                  <div className="lego-brick-header bg-lego-black flex items-center">
                    <span className="font-black text-sm text-white">BESTEHENDE KATEGORIEN</span>
                  </div>
                  <div className="lego-brick bg-white overflow-x-auto">
                    <table className="min-w-full text-left border-collapse text-xs">
                      <thead>
                        <tr className="bg-lego-gray-light border-b-2 border-lego-black font-extrabold text-lego-black">
                          <th className="p-3 w-44">Name</th>
                          <th className="p-3 w-40">Slug</th>
                          <th className="p-3">Beschreibung</th>
                          <th className="p-3 w-24 text-right">Löschen</th>
                        </tr>
                      </thead>
                      <tbody>
                        {categories.map(cat => (
                          <tr key={cat.id} className="border-b border-lego-gray-light hover:bg-lego-gray-light/30">
                            <td className="p-3 font-bold text-lego-black w-44 whitespace-nowrap">{cat.name}</td>
                            <td className="p-3 font-mono w-40 whitespace-nowrap">{cat.slug}</td>
                            <td className="p-3 text-lego-gray-dark break-words">{cat.description || '-'}</td>
                            <td className="p-3 text-right w-24">
                              <button onClick={() => handleDeleteCategory(cat.id)} className="lego-btn lego-btn-red py-0.5 px-2 text-[10px] whitespace-nowrap">
                                Löschen
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

              </div>
            )}

            {/* 3. TAGS TAB */}
            {activeTab === 'tags' && (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                
                {/* Left: Create form */}
                <div className="flex flex-col">
                  <div className="lego-brick-header bg-lego-green flex items-center">
                    <span className="font-black text-sm text-white">NEUER TAG (SCHLAGWORT)</span>
                  </div>
                  <form onSubmit={handleCreateTag} className="lego-brick bg-white p-6 flex flex-col gap-5">
                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold uppercase text-lego-gray-dark">Tag Name</label>
                      <input
                        type="text"
                        required
                        value={tagName}
                        onChange={(e) => setTagName(e.target.value)}
                        className="lego-input text-sm"
                        placeholder="z.B. Breaking"
                      />
                    </div>
                    <button type="submit" className="lego-btn lego-btn-green w-full justify-center text-sm py-2.5 mt-2">
                      Speichern
                    </button>
                  </form>
                </div>

                {/* Right: Table list */}
                <div className="md:col-span-2 flex flex-col">
                  <div className="lego-brick-header bg-lego-black flex items-center">
                    <span className="font-black text-sm text-white">BESTEHENDE TAGS</span>
                  </div>
                  <div className="lego-brick bg-white overflow-x-auto">
                    <table className="min-w-full text-left border-collapse text-xs">
                      <thead>
                        <tr className="bg-lego-gray-light border-b-2 border-lego-black font-extrabold text-lego-black">
                          <th className="p-3 w-48">Name</th>
                          <th className="p-3 w-48">Slug</th>
                          <th className="p-3 w-24 text-right">Löschen</th>
                        </tr>
                      </thead>
                      <tbody>
                        {tags.map(tag => (
                          <tr key={tag.id} className="border-b border-lego-gray-light hover:bg-lego-gray-light/30">
                            <td className="p-3 font-bold text-lego-black w-48 whitespace-nowrap">#{tag.name}</td>
                            <td className="p-3 font-mono w-48 whitespace-nowrap">{tag.slug}</td>
                            <td className="p-3 text-right w-24">
                              <button onClick={() => handleDeleteTag(tag.id)} className="lego-btn lego-btn-red py-0.5 px-2 text-[10px] whitespace-nowrap">
                                Löschen
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

              </div>
            )}
          </>
        )}

      </main>

      {/* ── ARTICLE EDIT/CREATE MODAL ───────────────────────────────────────────── */}
      {showArticleForm && (
        <div className="lego-modal-overlay">
          <div className="lego-modal max-w-2xl w-full">
            <div className="lego-brick-header bg-lego-blue flex justify-between items-center">
              <span className="font-black text-white">
                {editingArticleId ? 'ARTIKEL BEARBEITEN' : 'NEUEN ARTIKEL ERSTELLEN'}
              </span>
              <button onClick={() => setShowArticleForm(false)} className="text-white hover:text-lego-yellow font-black transition-colors">X</button>
            </div>
            <form onSubmit={handleSaveArticle} className="p-6 flex flex-col gap-5 overflow-y-auto flex-grow">
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="flex flex-col gap-1">
                  <label className="text-xs font-bold uppercase text-lego-gray-dark">Titel *</label>
                  <input
                    type="text"
                    required
                    value={artTitle}
                    onChange={(e) => setArtTitle(e.target.value)}
                    className="lego-input text-sm"
                    placeholder="Haupttitel..."
                  />
                </div>
                <div className="flex flex-col gap-1">
                  <label className="text-xs font-bold uppercase text-lego-gray-dark">Untertitle *</label>
                  <input
                    type="text"
                    required
                    value={artSubtitle}
                    onChange={(e) => setArtSubtitle(e.target.value)}
                    className="lego-input text-sm"
                    placeholder="Kurzer Einleitungstext..."
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="flex flex-col gap-1">
                  <label className="text-xs font-bold uppercase text-lego-gray-dark">Reporter / Autor *</label>
                  <input
                    type="text"
                    required
                    value={artAuthor}
                    onChange={(e) => setArtAuthor(e.target.value)}
                    className="lego-input text-sm"
                  />
                </div>
                <div className="flex flex-col gap-1">
                  <label className="text-xs font-bold uppercase text-lego-gray-dark">Kategorie *</label>
                  <select
                    required
                    value={artCategoryId}
                    onChange={(e) => setArtCategoryId(e.target.value)}
                    className="lego-input text-sm font-semibold h-11"
                  >
                    <option value="" disabled>Auswählen...</option>
                    {categories.map(c => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Cover Image URL and File Upload */}
              <div className="border-3 border-lego-black p-4 rounded-lg bg-lego-gray-light/30 flex flex-col gap-3">
                <span className="text-xs font-black uppercase text-lego-black">📸 ARTIKELBILD</span>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="flex flex-col gap-1">
                    <label className="text-[10px] font-bold uppercase text-lego-gray-dark">Direkt hochladen</label>
                    <input
                      type="file"
                      accept="image/*"
                      onChange={handleImageUpload}
                      disabled={uploadingImage}
                      className="text-xs"
                    />
                    {uploadingImage && <span className="text-xs text-lego-blue font-bold">Lade hoch...</span>}
                  </div>
                  <div className="flex flex-col gap-1">
                    <label className="text-[10px] font-bold uppercase text-lego-gray-dark">Bild URL (automatisch gefüllt oder manuell)</label>
                    <input
                      type="text"
                      value={artCoverUrl}
                      onChange={(e) => setArtCoverUrl(e.target.value)}
                      className="lego-input text-xs"
                      placeholder="/uploads/images/filename.jpg"
                    />
                  </div>
                </div>
                {artCoverUrl && (
                  <div className="w-32 h-20 border-2 border-lego-black rounded overflow-hidden mt-2">
                    <img src={artCoverUrl} alt="Vorschau" className="w-full h-full object-cover" />
                  </div>
                )}
              </div>

              {/* Tags checkboxes */}
              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold uppercase text-lego-gray-dark">Tags (Schlagworte)</label>
                <div className="flex flex-wrap gap-2 mt-1">
                  {tags.length === 0 ? (
                    <span className="text-xs text-lego-gray-dark">Keine Tags in Datenbank vorhanden.</span>
                  ) : (
                    tags.map(t => {
                      const isSelected = artTagIds.includes(t.id);
                      return (
                        <button
                          key={t.id}
                          type="button"
                          onClick={() => handleTagToggle(t.id)}
                          className={`lego-btn py-0.5 px-2 text-[10px] ${isSelected ? 'lego-btn-yellow' : 'lego-btn-black'}`}
                        >
                          #{t.name}
                        </button>
                      );
                    })
                  )}
                </div>
              </div>

              {/* Content text */}
              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold uppercase text-lego-gray-dark">Artikeltext *</label>
                <textarea
                  required
                  value={artContent}
                  onChange={(e) => setArtContent(e.target.value)}
                  className="lego-input text-sm h-48 font-normal"
                  placeholder="Schreibe hier den kompletten Artikeltext..."
                />
              </div>

              {/* Actions */}
              <div className="mt-4 flex gap-3 border-t-3 border-lego-black/10 pt-4">
                <button type="submit" className="lego-btn lego-btn-blue flex-grow justify-center text-sm py-2">
                  Speichern
                </button>
                <button type="button" onClick={() => setShowArticleForm(false)} className="lego-btn lego-btn-black text-sm py-2">
                  Abbrechen
                </button>
              </div>

            </form>
          </div>
        </div>
      )}

    </div>
  );
}
