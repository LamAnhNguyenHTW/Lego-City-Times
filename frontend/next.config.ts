import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/api/v1/:path*",
        destination: "http://localhost:80/api/v1/:path*",
      },
      {
        source: "/uploads/:path*",
        destination: "http://localhost:80/uploads/:path*",
      },
    ];
  },
};

export default nextConfig;
