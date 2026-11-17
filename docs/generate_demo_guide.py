#!/usr/bin/env python3
"""
Generate the short demo/update guide PDF for the Attendance Management System.

Usage:
    python docs/generate_demo_guide.py
"""
from __future__ import annotations

from pathlib import Path
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT, TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
    PageBreak,
    ListFlowable,
    ListItem,
    HRFlowable,
)

OUT = Path(__file__).resolve().parent / "Attendance-System-Demo-Guide.pdf"

NAVY = colors.HexColor("#1F3864")
BLUE = colors.HexColor("#2E75B6")
LIGHT = colors.HexColor("#EAF1FB")
GREY = colors.HexColor("#666666")
GREEN = colors.HexColor("#2E7D32")
RED = colors.HexColor("#C0392B")

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="CoverTitle", fontName="Helvetica-Bold", fontSize=30, leading=36, textColor=NAVY, alignment=TA_LEFT, spaceAfter=8))
styles.add(ParagraphStyle(name="CoverSub", fontName="Helvetica", fontSize=14, leading=19, textColor=BLUE, spaceAfter=18))
styles.add(ParagraphStyle(name="H1", fontName="Helvetica-Bold", fontSize=17, leading=22, textColor=NAVY, spaceBefore=10, spaceAfter=8))
styles.add(ParagraphStyle(name="H2", fontName="Helvetica-Bold", fontSize=12.5, leading=16, textColor=BLUE, spaceBefore=10, spaceAfter=5))
styles.add(ParagraphStyle(name="Body", fontName="Helvetica", fontSize=10, leading=14.5, textColor=colors.HexColor("#222222"), spaceAfter=5))
styles.add(ParagraphStyle(name="BulletCustom", fontName="Helvetica", fontSize=10, leading=14, leftIndent=14, bulletIndent=3, spaceAfter=3))
styles.add(ParagraphStyle(name="Small", fontName="Helvetica", fontSize=8.5, leading=11, textColor=GREY))
styles.add(ParagraphStyle(name="Cell", fontName="Helvetica", fontSize=9, leading=12))
styles.add(ParagraphStyle(name="CellBold", fontName="Helvetica-Bold", fontSize=9, leading=12, textColor=colors.white))
styles.add(ParagraphStyle(name="CodeBlock", fontName="Courier", fontSize=8.2, leading=11, textColor=colors.HexColor("#1A1A1A"), backColor=colors.HexColor("#F4F6F8"), borderColor=colors.HexColor("#D0D7DE"), borderWidth=0.5, borderPadding=5, leftIndent=4, spaceAfter=6))


def P(text: str, style: str = "Body") -> Paragraph:
    return Paragraph(text, styles[style])


def bullets(items: list[str]) -> ListFlowable:
    return ListFlowable(
        [ListItem(Paragraph(escape(i) if "<" not in i else i, styles["BulletCustom"]), leftIndent=14) for i in items],
        bulletType="bullet",
        start="•",
        bulletFontName="Helvetica",
        bulletFontSize=9,
        leftIndent=14,
    )


def numbered(items: list[str]) -> ListFlowable:
    return ListFlowable(
        [ListItem(Paragraph(escape(i) if "<" not in i else i, styles["BulletCustom"]), leftIndent=18) for i in items],
        bulletType="1",
        bulletFontName="Helvetica-Bold",
        bulletFontSize=10,
        leftIndent=18,
    )


def code(text: str) -> Table:
    t = Table([[Paragraph(escape(text).replace("\n", "<br/>"), styles["CodeBlock"])]], colWidths=[170 * mm])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#F4F6F8")),
        ("BOX", (0, 0), (-1, -1), 0.5, colors.HexColor("#D0D7DE")),
        ("LEFTPADDING", (0, 0), (-1, -1), 8),
        ("RIGHTPADDING", (0, 0), (-1, -1), 8),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    return t


def kv_table(rows: list[tuple[str, str]]) -> Table:
    data = [[P("<b>" + escape(k) + "</b>", "Cell"), P(escape(v), "Cell")] for k, v in rows]
    t = Table(data, colWidths=[42 * mm, 128 * mm])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (0, -1), LIGHT),
        ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor("#B8CCE4")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 7),
        ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    return t


def header_footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(GREY)
    canvas.drawString(18 * mm, 12 * mm, "Attendance Management System - Demo & Update Guide")
    canvas.drawRightString(192 * mm, 12 * mm, f"Page {doc.page}")
    canvas.setStrokeColor(colors.HexColor("#D0D7DE"))
    canvas.setLineWidth(0.5)
    canvas.line(18 * mm, 15 * mm, 192 * mm, 15 * mm)
    canvas.restoreState()


def build() -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    doc = BaseDocTemplate(
        str(OUT),
        pagesize=A4,
        leftMargin=18 * mm,
        rightMargin=18 * mm,
        topMargin=18 * mm,
        bottomMargin=20 * mm,
        title="Attendance Management System - Demo & Update Guide",
        author="Attendance Management System",
        subject="Short demonstration and database update guide",
    )
    frame = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="main")
    doc.addPageTemplates([PageTemplate(id="all", frames=[frame], onPage=header_footer)])

    story = []

    # ---------------- Cover ----------------
    story.append(Spacer(1, 40 * mm))
    story.append(P("Attendance Management System", "CoverTitle"))
    story.append(P("Short Demo & Update Guide", "CoverSub"))
    story.append(HRFlowable(width="100%", thickness=1.2, color=BLUE, spaceAfter=14))
    story.append(P(
        "A Java 17 + Spring Boot + MySQL application for managing students, courses, subjects, "
        "attendance sessions, reports and users. This guide is written for a live demonstration "
        "and for updating the database afterwards.", "Body"))
    story.append(Spacer(1, 8 * mm))

    story.append(P("At a glance", "H2"))
    story.append(kv_table([
        ("Open the app", "http://localhost:8080"),
        ("Administrator", "admin / admin123"),
        ("Faculty", "faculty / faculty123"),
        ("One-click setup", "Run-Setup.bat"),
        ("Start", "Run-AttendanceApp.bat"),
        ("Stop", "Stop-AttendanceApp.bat"),
        ("Setup log", "setup.log"),
    ]))
    story.append(Spacer(1, 6 * mm))
    story.append(P("Python is not required. The application is Java + MySQL.", "Small"))

    story.append(PageBreak())

    # ---------------- Demo flow ----------------
    story.append(P("1. Five-minute demo flow", "H1"))
    story.append(P("Follow these steps in order. Each step maps to one screen in the application.", "Body"))

    steps = [
        "Login as admin / admin123. The Dashboard shows totals and overall attendance.",
        "Open Students. Search for 'Alice' and filter by course/semester/section. Add a new student.",
        "Open Courses and Subjects. Add a course, then a subject linked to that course and semester.",
        "Open Attendance. Choose course, semester, section, subject and date, then click Load Students.",
        "Mark one student Absent and the rest Present. Click Save Attendance to create the session.",
        "Reopen the same session with View/Edit and show that the saved marks are restored.",
        "Open Reports. Generate a student-wise report for S1001, then click Refresh Defaulters.",
        "Export the report to CSV.",
        "Open Users. Create a faculty user. Log out and log in as that user to show the reduced menu.",
        "Open Settings and change the threshold from 75 to 80. Show that defaulters recalculate.",
    ]
    story.append(numbered(steps))
    story.append(Spacer(1, 4 * mm))
    story.append(P("Tip: cancelled sessions are excluded from attendance percentages, reports and defaulters.", "Small"))

    story.append(P("Start / stop", "H2"))
    story.append(bullets([
        "Start: double-click Run-AttendanceApp.bat (or run Setup-AttendanceApp.ps1 once).",
        "Stop: double-click Stop-AttendanceApp.bat. It only stops the Spring Boot process, not MySQL.",
        "Logs: app.out.log, app.err.log, setup.log.",
    ]))

    story.append(PageBreak())

    # ---------------- Add / update data ----------------
    story.append(P("2. How to add and update data", "H1"))
    story.append(P("Use the web UI for normal work. Use SQL only for bulk or administrative changes. "
                   "Always back up before bulk edits.", "Body"))

    story.append(P("A. Web UI (recommended)", "H2"))
    story.append(bullets([
        "Students -> Add Student: student ID, name, email, course, semester, section -> Save.",
        "Courses -> Add Course: code, name, department.",
        "Subjects -> Add Subject: code, name, course, semester. The course must already exist.",
        "Attendance: select course -> semester -> section -> subject -> date -> Load Students -> mark -> Save.",
        "Users (admin): Add User -> username, full name, email, password, role, enabled.",
        "Settings (admin): attendance threshold (default 75).",
    ]))

    story.append(P("B. SQL examples", "H2"))
    story.append(P("Connect with: mysql -u root -p attendance_db", "Small"))
    story.append(code(
        "INSERT INTO courses (code, name, department)\n"
        "VALUES ('CS303', 'Database Systems', 'Computer Science');\n\n"
        "INSERT INTO subjects (code, name, course_code, semester)\n"
        "VALUES ('CS303', 'Database Systems', 'CS303', 'Fall 2026');\n\n"
        "INSERT INTO students (student_id, name, email, course, semester, section)\n"
        "VALUES ('S1009', 'Ivy Chen', 'ivy.chen@example.edu', 'CS101', 'Fall 2026', 'A');\n\n"
        "UPDATE students SET section = 'B' WHERE student_id = 'S1009';\n\n"
        "INSERT INTO settings (setting_key, setting_value)\n"
        "VALUES ('attendance.threshold', '80')\n"
        "ON DUPLICATE KEY UPDATE setting_value = '80';"
    ))
    story.append(P("Never insert a plain-text password into users.password. Use the Users page or the API so BCrypt hashes it.", "Small"))

    story.append(P("C. REST API", "H2"))
    story.append(P(
        "The API is available under /api. Unsafe methods need the CSRF token from "
        "GET /api/auth/csrf-token and the session cookie. The browser UI already handles this. "
        "Use the API only for integrations or automated tests.", "Body"))

    story.append(PageBreak())

    # ---------------- Common tasks / update / backup ----------------
    story.append(P("3. Common tasks", "H1"))
    tasks = [
        ("Add a student", "Students -> Add Student"),
        ("Add a course", "Courses -> Add Course"),
        ("Add a subject", "Subjects -> Add Subject"),
        ("Mark attendance", "Attendance -> Load Students -> Save Attendance"),
        ("Change threshold", "Settings (admin)"),
        ("Add a user", "Users (admin)"),
        ("Reset a password", "Users -> edit -> new password -> Save"),
        ("Export a report", "Reports -> Export CSV"),
    ]
    data = [[P("<b>Task</b>", "CellBold"), P("<b>Where</b>", "CellBold")]]
    data += [[P(escape(a), "Cell"), P(escape(b), "Cell")] for a, b in tasks]
    t = Table(data, colWidths=[50 * mm, 120 * mm])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor("#B8CCE4")),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, LIGHT]),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 7),
        ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    story.append(t)

    story.append(P("4. Updating the project", "H1"))
    story.append(bullets([
        "Pull the new code: git pull.",
        "Run Run-Setup.bat again. It keeps existing data, rebuilds, runs tests and restarts the app.",
        "For schema changes, update database/schema.sql and run: mysql -u root -p attendance_db < database\\schema.sql.",
        "For a destructive migration, back up first and write a separate migration script.",
    ]))

    story.append(P("5. Backup and restore", "H1"))
    story.append(code(
        "mysqldump -u root -p --databases attendance_db > attendance_db_backup.sql\n"
        "mysql -u root -p < attendance_db_backup.sql"
    ))

    story.append(P("6. Quick troubleshooting", "H1"))
    trouble = [
        ("Login fails", "Reset the password from Users (admin) or re-run setup on a fresh DB."),
        ("Attendance dropdowns empty", "Add at least one student for that course/semester."),
        ("Port 8080 in use", "Stop the other process or change SERVER_PORT in .env."),
        ("Port 3306 in use", "The script reuses the existing MySQL. Enter the root password if asked."),
        ("Changes not visible", "Hard-refresh with Ctrl+F5."),
    ]
    data = [[P("<b>Problem</b>", "CellBold"), P("<b>Fix</b>", "CellBold")]]
    data += [[P(escape(a), "Cell"), P(escape(b), "Cell")] for a, b in trouble]
    t = Table(data, colWidths=[45 * mm, 125 * mm])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor("#B8CCE4")),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, LIGHT]),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 7),
        ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    story.append(t)

    story.append(Spacer(1, 6 * mm))
    story.append(HRFlowable(width="100%", thickness=0.8, color=colors.HexColor("#D0D7DE"), spaceAfter=5))
    story.append(P("Full documentation: README.md. Default URL: http://localhost:8080", "Small"))

    doc.build(story)
    print(f"PDF written to {OUT}")


if __name__ == "__main__":
    build()