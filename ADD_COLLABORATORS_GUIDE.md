# 👥 Adding Collaborators to JARVIS Launcher

## 📋 Prerequisites

- ✅ Repository must be pushed to GitHub first
- ✅ You must be the repository owner
- ✅ Repository must exist at: `https://github.com/Debajeet-1411/JarvisLauncher`

---

## 🎯 Method 1: Add Collaborators via GitHub Website (Recommended)

### Step 1: Navigate to Repository Settings

1. Go to your repository: `https://github.com/Debajeet-1411/JarvisLauncher`
2. Click **"Settings"** tab (top menu bar)
3. In the left sidebar, click **"Collaborators and teams"**
    - You may need to enter your GitHub password to confirm

### Step 2: Add a Collaborator

1. Click the **"Add people"** button (green button)
2. Enter their:
    - **GitHub username** (e.g., `johndoe`)
    - **Email address** (if you don't know their username)
3. Select the correct person from the dropdown
4. Click **"Add [username] to this repository"**

### Step 3: Set Permission Level

When adding a collaborator, you'll see permission options:

**Available Roles:**

| Role | Permissions | Use Case |
|------|-------------|----------|
| **Read** | View code, clone, open issues | External reviewers |
| **Triage** | Read + manage issues/PRs | Community managers |
| **Write** | Read + push to repo | Regular developers |
| **Maintain** | Write + manage settings (no delete) | Project managers |
| **Admin** | Full access (delete, transfer) | Co-owners |

**For Admin Privileges:**

- Select **"Admin"** from the dropdown
- This gives them full control (except deleting your account)

### Step 4: Send Invitation

1. GitHub sends an email invitation to the collaborator
2. They must **accept the invitation** via:
    - Email link
    - GitHub notifications
    - Repository page (banner at top)

---

## 🚀 Method 2: Using GitHub CLI

If you have GitHub CLI installed:

```powershell
# Add a collaborator with Write access
gh api repos/Debajeet-1411/JarvisLauncher/collaborators/USERNAME \
  --method PUT \
  --field permission=push

# Add a collaborator with Admin access
gh api repos/Debajeet-1411/JarvisLauncher/collaborators/USERNAME \
  --method PUT \
  --field permission=admin
```

Replace `USERNAME` with their GitHub username.

---

## 👑 Giving Admin Privileges to Someone

### Option A: During Initial Invitation (When Adding)

1. Go to **Settings → Collaborators and teams**
2. Click **"Add people"**
3. Enter their username/email
4. **Choose "Admin" from the Role dropdown**
5. Click **"Add [username] to this repository"**

### Option B: Changing Existing Collaborator Role

1. Go to **Settings → Collaborators and teams**
2. Find the collaborator in the list
3. Click the **dropdown menu** next to their name
4. Select **"Admin"**
5. Changes apply immediately

---

## 📊 Permission Levels Explained

### 🟢 **Admin Access** (Full Control)

✅ All Write permissions  
✅ Manage repository settings  
✅ Add/remove collaborators  
✅ Manage GitHub Actions secrets  
✅ Manage webhooks and integrations  
✅ Delete repository  
✅ Transfer ownership  
✅ Manage branch protection rules  
✅ Publish releases

⚠️ **Cannot:**

- Delete your GitHub account
- Access billing settings (on paid plans)

### 🔵 **Write Access** (Developer)

✅ Push to repository  
✅ Create/merge pull requests  
✅ Manage issues and projects  
✅ Create releases  
✅ Edit wiki

❌ **Cannot:**

- Change repository settings
- Add collaborators
- Delete repository

### 🟡 **Read Access** (Viewer)

✅ View code  
✅ Clone repository  
✅ Open issues  
✅ Comment on PRs

❌ **Cannot:**

- Push code
- Close issues
- Merge PRs

---

## 🔐 Best Practices for Collaboration

### 1. **Choose the Right Role**

```
Trusted Co-owner → Admin
Developer → Write
Code Reviewer → Triage
External Viewer → Read
```

### 2. **Use Branch Protection Rules**

Protect your `main` branch from accidental changes:

1. **Settings → Branches → Add branch protection rule**
2. Branch name pattern: `main`
3. Enable:
    - ✅ Require pull request reviews before merging
    - ✅ Require status checks to pass
    - ✅ Require conversation resolution
    - ✅ Include administrators (to follow same rules)
4. Click **"Create"**

### 3. **Enable Two-Factor Authentication (2FA)**

For security, require 2FA for all collaborators:

1. **Settings → Code security and analysis**
2. Enable **"Require two-factor authentication"**

### 4. **Review Collaborators Periodically**

Remove inactive collaborators:

1. **Settings → Collaborators and teams**
2. Click **"Remove"** next to inactive users

---

## 👥 Adding Multiple Collaborators

### For Small Teams (< 5 people)

- Add as individual collaborators (see Method 1)

### For Larger Teams

- Create a **GitHub Organization** (free)
- Add repository to the organization
- Manage permissions via **Teams**

**Create Organization:**

1. Click your profile → **Settings**
2. Left sidebar → **Organizations**
3. Click **"New organization"**
4. Choose **"Free"** plan
5. Transfer your repository to the org

---

## 📧 Inviting Collaborators - Example Email

```text
Subject: Invitation to Collaborate on JARVIS Launcher 🤖

Hi [Name],

I've added you as an Admin collaborator on my GitHub repository:
JARVIS Launcher - AI-powered Android Launcher

Repository: https://github.com/Debajeet-1411/JarvisLauncher

Please:
1. Check your email for the GitHub invitation
2. Click "View invitation" and accept
3. Clone the repository:
   git clone https://github.com/Debajeet-1411/JarvisLauncher.git

You have full admin access, so you can:
- Push code directly
- Manage settings
- Add more collaborators

Looking forward to collaborating!

Best,
Debajeet
```

---

## 🔍 Verifying Collaborator Access

### Check Who Has Access

1. **Settings → Collaborators and teams**
2. View list of all collaborators with their roles

### As a Collaborator (After Accepting)

They should see:

- Repository in their GitHub dashboard
- "Push" access when cloning
- Settings tab (if Admin)

---

## 🛠️ Troubleshooting

### Problem: "User not found"

**Solution:**

- Check spelling of username
- Use their email address instead
- Ask them to create a GitHub account first

### Problem: "Invitation not received"

**Solution:**

- Check spam folder
- Go directly to repo: `https://github.com/Debajeet-1411/JarvisLauncher`
- Banner at top will show pending invitation
- Click "View invitation"

### Problem: "Can't access Settings tab"

**Solution:**

- Only repository owner sees Settings
- Or user must have Admin role

### Problem: "Can't push code"

**Solution:**

- Ensure invitation was accepted
- Check they have at least Write permission
- Verify they're using correct credentials

---

## 🔄 Workflow for Collaboration

### Recommended Git Workflow with Collaborators

```powershell
# Collaborator clones the repository
git clone https://github.com/Debajeet-1411/JarvisLauncher.git
cd JarvisLauncher

# Create a feature branch
git checkout -b feature/new-ui

# Make changes and commit
git add .
git commit -m "Add new UI feature"

# Push branch to GitHub
git push origin feature/new-ui

# Create Pull Request on GitHub
# Review and merge into main
```

### Branch Strategy

```
main (protected)
 ├── develop (development branch)
 │   ├── feature/new-ai-model
 │   ├── feature/gesture-controls
 │   └── bugfix/crash-on-launch
 └── hotfix/critical-bug
```

---

## 📚 Quick Reference Commands

```powershell
# Clone repository (as collaborator)
git clone https://github.com/Debajeet-1411/JarvisLauncher.git

# Check remote URL
git remote -v

# Pull latest changes
git pull origin main

# Create feature branch
git checkout -b feature/my-feature

# Push feature branch
git push origin feature/my-feature

# Delete branch after merge
git branch -d feature/my-feature
git push origin --delete feature/my-feature
```

---

## 🎓 Collaboration Tips

### For Repository Owner (You)

1. **Review code before merging** (enable PR reviews)
2. **Write clear commit messages**
3. **Use GitHub Issues** for task tracking
4. **Create a CONTRIBUTING.md** file with guidelines
5. **Respond to collaborator questions** promptly

### For Collaborators

1. **Always pull before starting work**
   ```powershell
   git pull origin main
   ```
2. **Create feature branches** (never push directly to main)
3. **Write descriptive commit messages**
4. **Test your code** before pushing
5. **Ask questions** via GitHub Issues

---

## 📋 Collaboration Checklist

Before adding collaborators:

- [ ] Repository is public or collaborators have GitHub accounts
- [ ] Branch protection rules are set up
- [ ] README.md has setup instructions
- [ ] CONTRIBUTING.md exists (optional but recommended)
- [ ] Issues/Projects are configured for task management
- [ ] Code of conduct is added (optional)

When adding collaborators:

- [ ] Choose appropriate permission level
- [ ] Send them repository link
- [ ] Share development setup guide
- [ ] Explain branch workflow
- [ ] Add them to group chat (if any)

---

## 🌟 Advanced: GitHub Teams

For organizations with many collaborators:

1. **Create Organization** (if not already)
2. **Settings → Teams → New team**
3. Add team (e.g., "Core Developers", "UI Team")
4. Add members to team
5. Grant team access to repository
6. Manage permissions at team level

**Benefits:**

- Single permission change affects all team members
- Easier to manage large groups
- Can create nested teams
- Discussion boards per team

---

## 📞 Need Help?

- GitHub
  Docs: https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/managing-repository-settings/managing-teams-and-people-with-access-to-your-repository
- GitHub Support: https://support.github.com/
- Community Forum: https://github.community/

---

## ✅ Summary

**To give someone Admin access:**

1. Push your repository to GitHub first
2. Go to: `https://github.com/Debajeet-1411/JarvisLauncher/settings/access`
3. Click **"Add people"**
4. Enter their GitHub username or email
5. Select **"Admin"** role
6. Click **"Add [username] to this repository"**
7. They accept invitation via email
8. Done! They now have full admin access

**Admin privileges include:**

- ✅ All code access
- ✅ Can modify repository settings
- ✅ Can add/remove other collaborators
- ✅ Can delete repository (use with caution!)
- ✅ Can transfer ownership

---

**Good luck with your collaboration! 🚀**
