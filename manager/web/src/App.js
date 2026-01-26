import { BrowserRouter, Routes, Route } from "react-router-dom";
import { HomePage } from "./pages/HomePage";
import { BlockedUsersPage } from "./pages/BlockedUsersPage";
import { IssuesPage } from "./pages/IssuesPage";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/blocked-users" element={<BlockedUsersPage />} />
        <Route path="/issues" element={<IssuesPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;