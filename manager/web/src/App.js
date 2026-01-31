import { BrowserRouter, Routes, Route } from "react-router-dom";
import { HomePage } from "./pages/HomePage";
import { BlockedUsersPage } from "./pages/BlockedUsersPage";
import { IssuesPage } from "./pages/IssuesPage";
import { LoginPage } from "./pages/LoginPage";
import { AddUserPage } from "./pages/AddUserPage";
import { AllUsersPage } from "./pages/AllUsersPage";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/accueil" element={<HomePage />} />
        <Route path="/blocked-users" element={<BlockedUsersPage />} />
        <Route path="/issues" element={<IssuesPage />} />
        <Route path="/" element={<LoginPage />} />
        <Route path="/add-user" element={<AddUserPage />} />
        <Route path="/all-users" element={<AllUsersPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;