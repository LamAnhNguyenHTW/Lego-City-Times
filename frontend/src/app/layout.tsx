import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "LEGO City News",
  description: "Das Nachrichtenportal von LEGO City: Polizei, Feuerwehr, Wirtschaft, Sport und mehr.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="de">
      <body>
        <div style={{ position: "relative", minHeight: "100vh" }}>
          {children}
        </div>
      </body>
    </html>
  );
}
