const API = "/api";
let csrfToken = "";
let csrfHeader = "X-XSRF-TOKEN";

async function initCsrf() {
    try {
        const res = await fetch(`${API}/auth/csrf-token`, { credentials: "same-origin" });
        if (res.ok) {
            const data = await res.json();
            csrfToken = data.token;
            csrfHeader = data.header;
        }
    } catch (e) {
        console.warn("CSRF token fetch failed", e);
    }
}

function headers(extra = {}) {
    const h = { "Content-Type": "application/json", ...extra };
    if (csrfToken) h[csrfHeader] = csrfToken;
    return h;
}

async function api(path, opts = {}) {
    const res = await fetch(`${API}${path}`, { ...opts, headers: headers(opts.headers), credentials: "same-origin" });
    if (res.status === 401) {
        window.location.href = "/login?session=expired";
        throw new Error("Unauthorized");
    }
    if (res.status === 403) {
        window.location.href = "/403";
        throw new Error("Forbidden");
    }
    const text = await res.text();
    let data = null;
    if (text) {
        try { data = JSON.parse(text); } catch { data = { message: text }; }
    }
    if (!res.ok) {
        const err = new Error(data && data.message ? data.message : `Request failed (${res.status})`);
        err.status = res.status;
        err.data = data;
        throw err;
    }
    return data;
}

function showAlert(containerId, type, message) {
    const container = document.getElementById(containerId);
    if (!container) return;
    container.innerHTML = `<div class="alert alert-${type}">${escapeHtml(message)}</div>`;
    setTimeout(() => { container.innerHTML = ""; }, 5000);
}

function escapeHtml(text) {
    if (!text) return "";
    return String(text).replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;" })[c]);
}

function formatDate(dateStr) {
    if (!dateStr) return "";
    return dateStr;
}

async function loadCurrentUser() {
    try {
        const data = await api("/auth/me");
        const el = document.getElementById("current-user");
        if (el) el.textContent = data.data.fullName || data.data.username;
        const roleBadge = document.getElementById("user-role");
        if (roleBadge) {
            roleBadge.textContent = data.data.role;
            roleBadge.className = "user-badge " + (data.data.role === "ADMIN" ? "" : "");
        }
        const isAdmin = document.body.dataset.isAdmin === "true" || data.data.role === "ADMIN";
        document.querySelectorAll("[data-admin-only]").forEach(el => {
            el.style.display = isAdmin ? "" : "none";
        });
    } catch (e) {
        console.warn("loadCurrentUser failed", e);
    }
}

async function logout() {
    try { await api("/auth/logout", { method: "POST" }); } catch (e) { /* ignore */ }
    window.location.href = "/login?logout";
}

// ---------------- Dashboard ----------------
async function initDashboard() {
    const statsContainer = document.getElementById("stats");
    const recentContainer = document.getElementById("recent-sessions");
    if (!statsContainer && !recentContainer) return;

    try {
        const stats = await api("/dashboard/stats");
        const d = stats.data;
        if (statsContainer) {
            statsContainer.innerHTML = `
                <div class="card"><div class="card-label">Total Students</div><div class="card-value">${d.totalStudents}</div></div>
                <div class="card"><div class="card-label">Attendance Sessions</div><div class="card-value">${d.totalSessions}</div></div>
                <div class="card"><div class="card-label">Overall Attendance %</div><div class="card-value">${d.overallPercentage}%</div></div>
                <div class="card"><div class="card-label">Students Below Threshold (${d.threshold}%)</div><div class="card-value card-delta bad">${d.belowThreshold}</div></div>
            `;
        }
        if (recentContainer) {
            const sessions = await api("/attendance/sessions");
            const recent = (sessions.data || []).slice(0, 5);
            recentContainer.innerHTML = recent.map(s => `
                <tr>
                    <td>${s.sessionDate}</td>
                    <td>${s.subjectCode}</td>
                    <td>${s.section}</td>
                    <td><span class="badge ${s.status === "active" ? "badge-active" : "badge-cancelled"}">${s.status}</span></td>
                    <td>${s.createdBy}</td>
                </tr>
            `).join("") || "<tr><td colspan='5' style='text-align:center;color:var(--text-muted);'>No sessions yet</td></tr>";
        }
    } catch (e) {
        console.error("dashboard load failed", e);
    }
}

// ---------------- Students ----------------
async function initStudents() {
    const listEl = document.getElementById("students-list");
    const form = document.getElementById("student-form");
    const search = document.getElementById("student-search");
    const filterCourse = document.getElementById("filter-course");
    const filterSemester = document.getElementById("filter-semester");
    const filterSection = document.getElementById("filter-section");
    const modal = document.getElementById("student-modal");
    const modalTitle = document.getElementById("modal-title");
    const cancelBtn = document.getElementById("modal-cancel");
    const deleteBtn = document.getElementById("delete-student-btn");
    const alertContainer = document.getElementById("alert-students");
    let editingId = null;

    const fields = {
        studentId: document.getElementById("s-student-id"),
        name: document.getElementById("s-name"),
        email: document.getElementById("s-email"),
        course: document.getElementById("s-course"),
        semester: document.getElementById("s-semester"),
        section: document.getElementById("s-section"),
    };

    async function load() {
        let url = "/students";
        const params = new URLSearchParams();
        if (search.value) params.set("search", search.value);
        if (filterCourse.value) params.set("course", filterCourse.value);
        if (filterSemester.value) params.set("semester", filterSemester.value);
        if (filterSection.value) params.set("section", filterSection.value);
        if (params.toString()) url += "?" + params.toString();
        const res = await api(url);
        const students = res.data || [];
        listEl.innerHTML = students.map(s => `
            <tr>
                <td>${escapeHtml(s.studentId)}</td>
                <td>${escapeHtml(s.name)}</td>
                <td>${escapeHtml(s.email || "-")}</td>
                <td>${escapeHtml(s.course)}</td>
                <td>${escapeHtml(s.semester)}</td>
                <td>${escapeHtml(s.section)}</td>
                <td>
                    <button class="btn-icon" onclick="editStudent(${s.id}, '${escapeJs(s.studentId)}', '${escapeJs(s.name)}', '${escapeJs(s.email || "")}', '${escapeJs(s.course)}', '${escapeJs(s.semester)}', '${escapeJs(s.section)}')" title="Edit">&#9998;</button>
                    <button class="btn-icon" onclick="confirmDeleteStudent(${s.id}, '${escapeJs(s.name)}')" title="Delete">&#10006;</button>
                </td>
            </tr>
        `).join("") || "<tr><td colspan='7' style='text-align:center;color:var(--text-muted);'>No students found</td></tr>";

        const courses = [...new Set(students.map(s => s.course))].sort();
        const semesters = [...new Set(students.map(s => s.semester))].sort();
        const sections = [...new Set(students.map(s => s.section))].sort();
        [filterCourse, filterSemester, filterSection].forEach((f, i) => {
            f.innerHTML = '<option value="">All</option>' + ([courses, semesters, sections][i]).map(v => `<option value="${escapeHtml(v)}">${escapeHtml(v)}</option>`).join("");
        });
    }

    form.onsubmit = async (e) => {
        e.preventDefault();
        const payload = {
            studentId: fields.studentId.value.trim(),
            name: fields.name.value.trim(),
            email: fields.email.value.trim(),
            course: fields.course.value.trim(),
            semester: fields.semester.value.trim(),
            section: fields.section.value.trim()
        };
        Object.values(fields).forEach(f => { f.classList.remove("invalid"); f.nextElementSibling && (f.nextElementSibling.style.display = "none"); });
        try {
            if (editingId) {
                await api(`/students/${editingId}`, { method: "PUT", body: JSON.stringify(payload) });
                showAlert(alertContainer.id, "success", "Student updated");
            } else {
                await api("/students", { method: "POST", body: JSON.stringify(payload) });
                showAlert(alertContainer.id, "success", "Student created");
            }
            closeModal();
            load();
        } catch (err) {
            if (err.status === 400 && err.data && err.data.data) {
                const errors = err.data.data;
                Object.keys(errors).forEach(k => {
                    const f = fields[k];
                    if (f) { f.classList.add("invalid"); const errEl = f.nextElementSibling; if (errEl && errEl.classList.contains("error-msg")) { errEl.textContent = errors[k]; errEl.style.display = "block"; } }
                });
            } else {
                showAlert(alertContainer.id, "error", err.message || "Failed to save student");
            }
        }
    };

    window.editStudent = (id, studentId, name, email, course, semester, section) => {
        editingId = id;
        modalTitle.textContent = "Edit Student";
        fields.studentId.value = studentId; fields.name.value = name;
        fields.email.value = email; fields.course.value = course;
        fields.semester.value = semester; fields.section.value = section;
        deleteBtn.style.display = "inline-block";
        modal.classList.add("open");
    };

    window.confirmDeleteStudent = (id, name) => {
        if (!confirm(`Delete student "${escapeHtml(name)}"? Related attendance records will be handled.`)) return;
        deleteStudent(id);
    };

    async function deleteStudent(id) {
        try {
            await api(`/students/${id}`, { method: "DELETE" });
            showAlert(alertContainer.id, "success", "Student deleted");
            load();
        } catch (err) {
            showAlert(alertContainer.id, "error", err.message || "Failed to delete student");
        }
    }

    cancelBtn.onclick = closeModal;
    modal.querySelector(".modal-close").onclick = closeModal;
    deleteBtn.onclick = () => { if (editingId) deleteStudent(editingId); closeModal(); };
    document.getElementById("btn-add-student").onclick = () => {
        editingId = null; modalTitle.textContent = "Add Student";
        Object.values(fields).forEach(f => { f.value = ""; f.classList.remove("invalid"); });
        document.querySelectorAll(".error-msg").forEach(el => el.style.display = "none");
        deleteBtn.style.display = "none";
        modal.classList.add("open");
    };

    function closeModal() { modal.classList.remove("open"); editingId = null; }
    search.oninput = load;
    filterCourse.onchange = load;
    filterSemester.onchange = load;
    filterSection.onchange = load;

    load();
}

// ---------------- Courses ----------------
async function initCourses() {
    const listEl = document.getElementById("courses-list");
    const form = document.getElementById("course-form");
    const alertContainer = document.getElementById("alert-courses");
    const codeIn = document.getElementById("c-code");
    const nameIn = document.getElementById("c-name");
    const deptIn = document.getElementById("c-department");
    let editingId = null;

    async function load() {
        const res = await api("/courses");
        const courses = res.data || [];
        listEl.innerHTML = courses.map(c => `
            <tr>
                <td>${escapeHtml(c.code)}</td>
                <td>${escapeHtml(c.name)}</td>
                <td>${escapeHtml(c.department || "-")}</td>
                <td>
                    <button class="btn-icon" onclick="editCourse(${c.id}, '${escapeJs(c.code)}', '${escapeJs(c.name)}', '${escapeJs(c.department || "")}')" title="Edit">&#9998;</button>
                    <button class="btn-icon" onclick="confirmDeleteCourse(${c.id}, '${escapeJs(c.code)}')" title="Delete">&#10006;</button>
                </td>
            </tr>
        `).join("") || "<tr><td colspan='4' style='text-align:center;color:var(--text-muted);'>No courses found</td></tr>";
    }

    form.onsubmit = async (e) => {
        e.preventDefault();
        const payload = { code: codeIn.value.trim(), name: nameIn.value.trim(), department: deptIn.value.trim() };
        try {
            if (editingId) await api(`/courses/${editingId}`, { method: "PUT", body: JSON.stringify(payload) });
            else await api("/courses", { method: "POST", body: JSON.stringify(payload) });
            showAlert(alertContainer.id, "success", editingId ? "Course updated" : "Course created");
            load();
            closeModal();
        } catch (err) { showAlert(alertContainer.id, "error", err.message || "Failed to save course"); }
    };

    window.editCourse = (id, code, name, dept) => {
        editingId = id; codeIn.value = code; nameIn.value = name; deptIn.value = dept;
        document.getElementById("course-modal-title").textContent = "Edit Course";
        document.getElementById("course-delete-btn").style.display = "inline-block";
        document.getElementById("course-modal").classList.add("open");
    };

    window.confirmDeleteCourse = (id, code) => { if (!confirm(`Delete course "${escapeHtml(code)}"?")) return; deleteCourse(id); };
    async function deleteCourse(id) {
        try { await api(`/courses/${id}`, { method: "DELETE" }); showAlert(alertContainer.id, "success", "Course deleted"); load(); }
        catch (err) { showAlert(alertContainer.id, "error", err.message || "Failed to delete course"); }
    }
    document.getElementById("course-modal-cancel").onclick = () => document.getElementById("course-modal").classList.remove("open");
    document.getElementById("course-modal").querySelector(".modal-close").onclick = () => document.getElementById("course-modal").classList.remove("open");
    document.getElementById("course-delete-btn").onclick = () => { if (editingId) deleteCourse(editingId); document.getElementById("course-modal").classList.remove("open"); };
    document.getElementById("btn-add-course").onclick = () => {
        editingId = null; document.getElementById("course-modal-title").textContent = "Add Course";
        codeIn.value = ""; nameIn.value = ""; deptIn.value = "";
        document.getElementById("course-delete-btn").style.display = "none";
        document.getElementById("course-modal").classList.add("open");
    };
    load();
}

// ---------------- Subjects ----------------
async function initSubjects() {
    const listEl = document.getElementById("subjects-list");
    const form = document.getElementById("subject-form");
    const alertContainer = document.getElementById("alert-subjects");
    const codeIn = document.getElementById("sub-code");
    const nameIn = document.getElementById("sub-name");
    const courseIn = document.getElementById("sub-course");
    const semIn = document.getElementById("sub-semester");
    let editingId = null;

    async function load() {
        const res = await api("/subjects");
        const subjects = res.data || [];
        listEl.innerHTML = subjects.map(s => `
            <tr>
                <td>${escapeHtml(s.code)}</td>
                <td>${escapeHtml(s.name)}</td>
                <td>${escapeHtml(s.courseCode)}</td>
                <td>${escapeHtml(s.semester)}</td>
                <td>
                    <button class="btn-icon" onclick="editSubject(${s.id}, '${escapeJs(s.code)}', '${escapeJs(s.name)}', '${escapeJs(s.courseCode)}', '${escapeJs(s.semester)}')" title="Edit">&#9998;</button>
                    <button class="btn-icon" onclick="confirmDeleteSubject(${s.id}, '${escapeJs(s.code)}')" title="Delete">&#10006;</button>
                </td>
            </tr>
        `).join("") || "<tr><td colspan='5' style='text-align:center;color:var(--text-muted);'>No subjects found</td></tr>";
    }

    form.onsubmit = async (e) => {
        e.preventDefault();
        const payload = { code: codeIn.value.trim(), name: nameIn.value.trim(), courseCode: courseIn.value.trim(), semester: semIn.value.trim() };
        try {
            if (editingId) await api(`/subjects/${editingId}`, { method: "PUT", body: JSON.stringify(payload) });
            else await api("/subjects", { method: "POST", body: JSON.stringify(payload) });
            showAlert(alertContainer.id, "success", editingId ? "Subject updated" : "Subject created");
            load(); closeModal();
        } catch (err) { showAlert(alertContainer.id, "error", err.message || "Failed to save subject"); }
    };

    window.editSubject = (id, code, name, course, sem) => {
        editingId = id; codeIn.value = code; nameIn.value = name; courseIn.value = course; semIn.value = sem;
        document.getElementById("subject-modal-title").textContent = "Edit Subject";
        document.getElementById("subject-delete-btn").style.display = "inline-block";
        document.getElementById("subject-modal").classList.add("open");
    };

    window.confirmDeleteSubject = (id, code) => { if (!confirm(`Delete subject "${escapeHtml(code)}"?")) return; deleteSubject(id); };
    async function deleteSubject(id) {
        try { await api(`/subjects/${id}`, { method: "DELETE" }); showAlert(alertContainer.id, "success", "Subject deleted"); load(); }
        catch (err) { showAlert(alertContainer.id, "error", err.message || "Failed to delete subject"); }
    }
    document.getElementById("subject-modal-cancel").onclick = () => document.getElementById("subject-modal").classList.remove("open");
    document.getElementById("subject-modal").querySelector(".modal-close").onclick = () => document.getElementById("subject-modal").classList.remove("open");
    document.getElementById("subject-delete-btn").onclick = () => { if (editingId) deleteSubject(editingId); document.getElementById("subject-modal").classList.remove("open"); };
    document.getElementById("btn-add-subject").onclick = () => {
        editingId = null; document.getElementById("subject-modal-title").textContent = "Add Subject";
        codeIn.value = ""; nameIn.value = ""; courseIn.value = ""; semIn.value = "";
        document.getElementById("subject-delete-btn").style.display = "none";
        document.getElementById("subject-modal").classList.add("open");
    };
    load();
}

// ---------------- Attendance ----------------
async function initAttendance() {
    const courseSel = document.getElementById("att-course");
    const subjectSel = document.getElementById("att-subject");
    const semesterSel = document.getElementById("att-semester");
    const sectionSel = document.getElementById("att-section");
    const dateIn = document.getElementById("att-date");
    const loadBtn = document.getElementById("btn-load-students");
    const studentsContainer = document.getElementById("att-students");
    const saveBtn = document.getElementById("btn-save-attendance");
    const alertContainer = document.getElementById("alert-attendance");
    const sessionIdInput = document.getElementById("att-session-id");
    const sessionInfo = document.getElementById("att-session-info");
    let currentSessionId = null;

    const today = new Date().toISOString().split("T")[0];
    dateIn.value = today;
    dateIn.max = today;

    async function loadCourses() {
        const res = await api("/courses");
        const courses = res.data || [];
        courseSel.innerHTML = '<option value="">Select course</option>' + courses.map(c => `<option value="${escapeHtml(c.code)}">${escapeHtml(c.code)} - ${escapeHtml(c.name)}</option>`).join("");
    }
    async function loadSubjects() {
        const cc = courseSel.value; const sem = semesterSel.value;
        subjectSel.innerHTML = '<option value="">Select subject</option>';
        if (!cc || !sem) return;
        const res = await api(`/subjects?courseCode=${encodeURIComponent(cc)}&semester=${encodeURIComponent(sem)}`);
        const subjects = res.data || [];
        subjectSel.innerHTML = '<option value="">Select subject</option>' + subjects.map(s => `<option value="${escapeHtml(s.code)}">${escapeHtml(s.code)} - ${escapeHtml(s.name)}</option>`).join("");
    }

    async function loadSessions() {
        const cc = courseSel.value; const sub = subjectSel.value; const sem = semesterSel.value; const sec = sectionSel.value; const date = dateIn.value;
        let url = "/attendance/sessions";
        const p = new URLSearchParams();
        if (cc) p.set("courseCode", cc);
        if (sub) p.set("subjectCode", sub);
        if (sem) p.set("semester", sem);
        if (sec) p.set("section", sec);
        if (date) p.set("date", date);
        if (p.toString()) url += "?" + p.toString();
        const res = await api(url);
        const sessions = res.data || [];
        sessionInfo.innerHTML = sessions.map(s => `
            <div class="card" style="padding:0.75rem;margin-bottom:0.5rem;">
                <strong>${s.subjectCode}</strong> - ${s.section} on ${s.sessionDate}
                <span class="badge ${s.status === "active" ? "badge-active" : "badge-cancelled"}">${s.status}</span>
                ${s.status === "active" ? `<button class="btn btn-sm btn-outline" onclick="openSession(${s.id})">View/Edit</button>` : ""}
            </div>
        `).join("") || "<div style='color:var(--text-muted);font-size:0.9rem;'>No sessions for this selection.</div>";
    }

    async function openSession(sessionId) {
        const res = await api(`/attendance/sessions/${sessionId}`);
        const s = res.data;
        currentSessionId = s.id;
        sessionIdInput.value = s.id;
        sessionInfo.innerHTML = `<div class="alert alert-info">Editing session: ${s.subjectCode} - ${s.section} on ${s.sessionDate} (${s.status})</div> <button class="btn btn-sm btn-danger" onclick="cancelSession(${s.id})">Cancel Session</button>`;
        await loadStudentsForSession(s);
    }

    async function loadStudentsForSession(s) {
        studentsContainer.innerHTML = "<div style='color:var(--text-muted);'>Loading students...</div>";
        const cc = s.courseCode, sem = s.semester, sec = s.section;
        const studentsRes = await api(`/students?course=${encodeURIComponent(cc)}&semester=${encodeURIComponent(sem)}&section=${encodeURIComponent(sec)}`);
        const students = studentsRes.data || [];
        const recordsRes = await api(`/attendance/sessions/${s.id}/records`);
        const records = recordsRes.data || [];
        const recordMap = {}; records.forEach(r => recordMap[r.studentId] = r.status);

        studentsContainer.innerHTML = students.map(st => {
            const status = recordMap[st.studentId] || "ABSENT";
            return `
            <div class="student-card">
                <div class="info"><div class="name">${escapeHtml(st.name)}</div><div class="sid">${escapeHtml(st.studentId)}</div></div>
                <div class="status-group">
                    <button type="button" class="status-btn ${status === "PRESENT" ? "active" : ""}" onclick="toggleStatus(this, 'PRESENT', '${escapeJs(st.studentId)}')">Present</button>
                    <button type="button" class="status-btn ${status === "ABSENT" ? "active" : ""}" onclick="toggleStatus(this, 'ABSENT', '${escapeJs(st.studentId)}')">Absent</button>
                </div>
            </div>`;
        }).join("") || "<div style='color:var(--text-muted);'>No students in this course/semester/section.</div>";
        saveBtn.disabled = students.length === 0;
    }

    window.toggleStatus = (btn, status, studentId) => {
        const parent = btn.parentElement;
        parent.querySelectorAll(".status-btn").forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
        btn.dataset.status = status;
        btn.dataset.student = studentId;
    };

    loadBtn.onclick = async () => {
        if (!courseSel.value || !subjectSel.value || !semesterSel.value || !sectionSel.value || !dateIn.value) {
            showAlert(alertContainer.id, "error", "Please fill course, subject, semester, section and date.");
            return;
        }
        currentSessionId = null; sessionIdInput.value = "";
        sessionInfo.innerHTML = "<div style='color:var(--text-muted);'>Select an existing session or click Save to create a new one.</div>";
        studentsContainer.innerHTML = "<div style='color:var(--text-muted);'>Loading students...</div>";
        try {
            const cc = courseSel.value, sem = semesterSel.value, sec = sectionSel.value;
            const studentsRes = await api(`/students?course=${encodeURIComponent(cc)}&semester=${encodeURIComponent(sem)}&section=${encodeURIComponent(sec)}`);
            const students = studentsRes.data || [];
            studentsContainer.innerHTML = students.map(st => `
            <div class="student-card">
                <div class="info"><div class="name">${escapeHtml(st.name)}</div><div class="sid">${escapeHtml(st.studentId)}</div></div>
                <div class="status-group">
                    <button type="button" class="status-btn active" onclick="toggleStatus(this, 'PRESENT', '${escapeJs(st.studentId)}')" data-status="PRESENT" data-student="${escapeJs(st.studentId)}">Present</button>
                    <button type="button" class="status-btn" onclick="toggleStatus(this, 'ABSENT', '${escapeJs(st.studentId)}')" data-status="ABSENT" data-student="${escapeJs(st.studentId)}">Absent</button>
                </div>
            </div>`).join("") || "<div style='color:var(--text-muted);'>No students in this course/semester/section.</div>";
            saveBtn.disabled = students.length === 0;
            await loadSessions();
        } catch (err) { showAlert(alertContainer.id, "error", err.message || "Failed to load students"); }
    };

    saveBtn.onclick = async () => {
        if (!currentSessionId) {
            if (!confirm("Create a new attendance session and save these marks?")) return;
        }
        const marks = [];
        document.querySelectorAll(".status-btn[data-status]").forEach(b => {
            marks.push({ studentId: b.dataset.student, studentName: b.closest(".student-card").querySelector(".name").textContent, status: b.dataset.status === "PRESENT" ? "PRESENT" : "ABSENT" });
        });
        if (marks.length === 0) { showAlert(alertContainer.id, "error", "No students to mark."); return; }
        const payload = {
            courseCode: courseSel.value, subjectCode: subjectSel.value, semester: semesterSel.value,
            section: sectionSel.value, sessionDate: dateIn.value
        };
        try {
            let sessionId = currentSessionId;
            if (!sessionId) {
                const created = await api("/attendance/sessions", { method: "POST", body: JSON.stringify(payload) });
                sessionId = created.data.id;
            }
            await api(`/attendance/sessions/${sessionId}/marks`, { method: "POST", body: JSON.stringify(marks) });
            showAlert(alertContainer.id, "success", "Attendance saved successfully");
            await openSession(sessionId);
        } catch (err) {
            showAlert(alertContainer.id, "error", err.message || "Failed to save attendance");
        }
    };

    window.cancelSession = async (id) => {
        if (!confirm("Cancel this attendance session?")) return;
        try {
            await api(`/attendance/sessions/${id}/cancel`, { method: "POST" });
            showAlert(alertContainer.id, "success", "Session cancelled");
            loadSessions();
        } catch (err) { showAlert(alertContainer.id, "error", err.message || "Failed to cancel"); }
    };

    courseSel.onchange = loadSubjects;
    semesterSel.onchange = loadSubjects;
    subjectSel.onchange = loadSessions;
    sectionSel.onchange = loadSessions;
    dateIn.onchange = loadSessions;

    loadCourses();
}

// ---------------- Reports ----------------
async function initReports() {
    const studentIdIn = document.getElementById("rep-student-id");
    const studentNameDatalist = document.getElementById("rep-student-names");
    const generateBtn = document.getElementById("btn-generate-report");
    const exportBtn = document.getElementById("btn-export-report");
    const resultContainer = document.getElementById("report-result");
    const alertContainer = document.getElementById("alert-reports");
    const defaultersBtn = document.getElementById("btn-defaulters");
    const defaultersContainer = document.getElementById("defaulters-result");

    async function loadStudents() {
        const res = await api("/students");
        const students = res.data || [];
        studentNameDatalist.innerHTML = students.map(s => `<option value="${escapeHtml(s.studentId)}">${escapeHtml(s.name)} (${escapeHtml(s.studentId)})</option>`).join("");
    }

    async function generate() {
        const studentId = studentIdIn.value.trim();
        if (!studentId) { showAlert(alertContainer.id, "error", "Enter a student ID."); return; }
        try {
            const res = await api(`/reports/student-wise?studentId=${encodeURIComponent(studentId)}`);
            const rows = res.data || [];
            resultContainer.innerHTML = `<table><thead><tr><th>Date</th><th>Course</th><th>Subject</th><th>Section</th><th>Student ID</th><th>Student Name</th><th>Status</th></tr></thead><tbody>${rows.map(r => `<tr><td>${escapeHtml(r.date)}</td><td>${escapeHtml(r.courseCode)}</td><td>${escapeHtml(r.subjectCode)}</td><td>${escapeHtml(r.section)}</td><td>${escapeHtml(r.studentId)}</td><td>${escapeHtml(r.studentName)}</td><td><span class="badge ${r.status === "PRESENT" ? "badge-present" : "badge-absent"}">${r.status}</span></td></tr>`).join("") || "<tr><td colspan='7' style='text-align:center;color:var(--text-muted);'>No attendance records</td></tr>"}</tbody></table>`;
        } catch (err) { showAlert(alertContainer.id, "error", err.message || "Failed to generate report"); }
    }

    async function defaulters() {
        try {
            const res = await api("/reports/defaulters");
            const rows = res.data || [];
            defaultersContainer.innerHTML = `<table><thead><tr><th>Student ID</th><th>Name</th><th>Total</th><th>Present</th><th>%</th><th>Threshold</th><th>Shortage</th></tr></thead><tbody>${rows.map(r => `<tr><td>${escapeHtml(r.studentId)}</td><td>${escapeHtml(r.studentName)}</td><td>${r.totalSessions}</td><td>${r.presentSessions}</td><td>${r.percentage.toFixed(1)}%</td><td>${r.threshold}%</td><td class="card-delta bad">${r.shortage}</td></tr>`).join("") || "<tr><td colspan='7' style='text-align:center;color:var(--text-muted);'>No defaulters</td></tr>"}</tbody></table>`;
        } catch (err) { showAlert(alertContainer.id, "error", err.message || "Failed to load defaulters"); }
    }

    generateBtn.onclick = generate;
    exportBtn.onclick = () => {
        const studentId = studentIdIn.value.trim();
        if (!studentId) { showAlert(alertContainer.id, "error", "Enter a student ID to export."); return; }
        window.location = `/reports/export-csv?type=student&studentId=${encodeURIComponent(studentId)}`;
    };
    defaultersBtn.onclick = defaulters;
    loadStudents();
}

// ---------------- Settings ----------------
async function initSettings() {
    const thresholdIn = document.getElementById("threshold");
    const thresholdForm = document.getElementById("threshold-form");
    const alertContainer = document.getElementById("alert-settings");
    const currentThreshold = document.getElementById("current-threshold");
    try {
        const res = await api("/settings/attendance-threshold");
        thresholdIn.value = res.data;
        if (currentThreshold) currentThreshold.textContent = res.data + "%";
    } catch (e) { console.warn("settings load failed", e); }
    thresholdForm.onsubmit = async (e) => {
        e.preventDefault();
        const val = parseInt(thresholdIn.value, 10);
        try {
            await api("/settings/attendance-threshold", { method: "POST", body: JSON.stringify({ threshold: val }) });
            showAlert(alertContainer.id, "success", "Threshold updated");
            if (currentThreshold) currentThreshold.textContent = val + "%";
        } catch (err) { showAlert(alertContainer.id, "error", err.message || "Failed to update threshold"); }
    };
}

// ---------------- Profile / Change password ----------------
async function initProfile() {
    const form = document.getElementById("password-form");
    const alertContainer = document.getElementById("alert-profile");
    if (!form) return;
    form.onsubmit = async (e) => {
        e.preventDefault();
        const newPass = document.getElementById("p-new").value;
        try {
            const me = await api("/auth/me");
            await api(`/users/${me.data.id}/password`, { method: "POST", body: JSON.stringify({ newPassword: newPass }) });
            showAlert(alertContainer.id, "success", "Password changed successfully");
            form.reset();
        } catch (err) { showAlert(alertContainer.id, "error", err.message || "Failed to change password"); }
    };
}

// ---------------- Users (admin) ----------------
async function initUsers() {
    const listEl = document.getElementById("users-list");
    if (!listEl) return;
    async function load() {
        const res = await api("/users");
        const users = res.data || [];
        listEl.innerHTML = users.map(u => `
            <tr>
                <td>${escapeHtml(u.username)}</td>
                <td>${escapeHtml(u.fullName)}</td>
                <td>${escapeHtml(u.email)}</td>
                <td><span class="badge">${u.role}</span></td>
                <td>${u.enabled ? "Active" : "Disabled"}</td>
                <td>
                    <button class="btn-icon" onclick="editUser(${u.id}, '${escapeJs(u.username)}', '${escapeJs(u.fullName)}', '${escapeJs(u.email)}', '${u.role}', ${u.enabled})" title="Edit">&#9998;</button>
                </td>
            </tr>
        `).join("") || "<tr><td colspan='6' style='text-align:center;color:var(--text-muted);'>No users</td></tr>";
    }
    load();
}
window.editUser = () => {};

function escapeJs(s) { return String(s == null ? "" : s).replace(/\\/g, "\\\\").replace(/'/g, "\\'").replace(/\n/g, "\\n"); }

// ---------------- Boot ----------------
document.addEventListener("DOMContentLoaded", async () => {
    await initCsrf();
    await loadCurrentUser();
    const page = document.body.dataset.page;
    if (page === "dashboard") initDashboard();
    else if (page === "students") initStudents();
    else if (page === "courses") initCourses();
    else if (page === "subjects") initSubjects();
    else if (page === "attendance") initAttendance();
    else if (page === "reports") initReports();
    else if (page === "settings") initSettings();
    else if (page === "profile") initProfile();
    else if (page === "users") initUsers();

    const logoutBtn = document.getElementById("btn-logout");
    if (logoutBtn) logoutBtn.onclick = logout;
});
