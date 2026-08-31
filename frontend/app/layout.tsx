import type { Metadata } from "next";
import "./globals.css";
import { UserProvider } from "@/features/auth/UserContext";

export const metadata: Metadata = {
  title: "SenusBoard",
  description: "Senus financial and operating performance dashboard",
};

/** Provides the document shell and global styles for every application route. */
export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>
        <UserProvider>{children}</UserProvider>
      </body>
    </html>
  );
}
