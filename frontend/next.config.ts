import type { NextConfig } from "next";

// Read the backend origin on the server and remove a trailing slash.
const apiUrl = (process.env.API_BASE_URL ?? "http://localhost:8080").replace(
  /\/$/,
  "",
);

const nextConfig: NextConfig = {
  async rewrites() {
    // Proxy API traffic through Next.js to preserve same-origin cookies.
    return [
      {
        source: "/api/:path*",
        destination: `${apiUrl}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
