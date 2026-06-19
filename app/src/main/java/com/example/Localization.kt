package com.example

object Localization {
    private val translations = mapOf(
        "app_title" to Pair("ProtectoNG", "پروتکتو ان‌جی"),
        "motto" to Pair("Guaranteed Privacy & Free Access to the Internet", "تضمین حریم خصوصی و دسترسی آزاد به اینترنت"),
        "home" to Pair("Home", "اتصال"),
        "servers" to Pair("Servers", "سرورها"),
        "configs" to Pair("Configs", "پیکربندی‌ها"),
        "stats" to Pair("Stats", "آمار مصرف"),
        "settings" to Pair("Settings", "تنظیمات"),
        "connected" to Pair("Connected", "متصل"),
        "connecting" to Pair("Connecting...", "در حال اتصال..."),
        "disconnected" to Pair("Disconnected", "قطع اتصال"),
        "disconnect_btn" to Pair("Disconnect", "قطع اتصال"),
        "connecting_btn" to Pair("Connecting...", "در حال اتصال..."),
        "quick_connect_btn" to Pair("Connect Now", "اتصال"),
        "download_speed" to Pair("Download Speed", "سرعت دانلود"),
        "upload_speed" to Pair("Upload Speed", "سرعت آپلود"),
        "connection_duration" to Pair("Duration", "مدت زمان اتصال"),
        "smart_config" to Pair("Active Config", "کانفیگ فعال"),
        "selected_server" to Pair("Selected Server", "سرور انتخاب شده"),
        "tap_select_server" to Pair("Tap to select profile...", "لمس کنید تا پروفایل انتخاب شود..."),
        "favorites" to Pair("Favorites", "برگزیده"),
        "not_selected" to Pair("Not Selected", "انتخاب نشده"),
        "secure_optimized" to Pair("Secure and Optimized Tunnels", "تونل‌های امن و بهینه شده"),
        
        // Servers Screen
        "server_search" to Pair("Search location...", "جستجو بین سرورها..."),
        "fav_servers" to Pair("Favorite Servers", "سرورهای مورد علاقه"),
        "all_servers" to Pair("All Servers List", "لیست همه سرورها"),
        "add_server_title" to Pair("Add New Server", "افزودن سرور جدید"),
        "ip_addr" to Pair("IP Address", "آدرس IP"),
        "ping_label" to Pair("Ping (Latency)", "پینگ (تأخیر)"),
        "add_btn" to Pair("Add", "افزودن"),
        "cancel_btn" to Pair("Cancel", "انصراف"),
        "no_servers_found" to Pair("No configurations imported yet.", "هنوز هیچ پیکربندی وارد نشده است."),
        
        // Configs Screen
        "manage_configs" to Pair("Manage Configurations", "مدیریت کانفیگ‌ها"),
        "configs_subtitle" to Pair("Active configurations with secure tunneling protocols", "پیکربندی‌های فعال با پروتکل‌های امن"),
        "active_config" to Pair("Active Config", "کانفیگ فعال"),
        "add_config_title" to Pair("Add New Configuration", "افزودن کانفیگ جدید"),
        "edit_config_title" to Pair("Edit Configuration", "ویرایش کانفیگ"),
        "config_name" to Pair("Configuration Name", "نام پیکربندی"),
        "protocol_type" to Pair("Protocol Type", "نوع پروتکل"),
        "config_addr" to Pair("Config Address (URL)", "آدرس کانفیگ (URL)"),
        "desc_notes" to Pair("Description / Note", "توضیحات / یادداشت"),
        "save_changes" to Pair("Save Changes", "ذخیره تغییرات"),
        "delete_config" to Pair("Delete Config", "حذف کانفیگ"),
        "placeholder_remarks" to Pair("High speed - no downtime", "سرعت بالا - بدون قطعی"),
        "placeholder_addr" to Pair("Enter config address URL...", "آدرس URL کانفیگ را وارد کنید..."),
        "placeholder_name" to Pair("e.g. Germany-VLESS", "مثلاً Germany-VLESS"),
        "import_via_clipboard" to Pair("Import from Clipboard", "وارد کردن از کلیپ‌بورد"),
        "import_via_file" to Pair("Import from File", "وارد کردن از فایل GP/JSON"),
        "import_via_qr" to Pair("Scan QR Code", "اسکن کد QR"),
        "choose_import_method" to Pair("Select Import Method", "روش بارگذاری کانفیگ"),
        "clipboard_not_found" to Pair("Clipboard is empty or contains invalid content", "حافظه موقت خالی است یا محتوای نامعتبر دارد"),
        "import_success_toast" to Pair("Configuration loaded and verified successfully!", "پیکربندی با موفقیت تحلیل و ذخیره شد!"),
        "import_failure_toast" to Pair("Unrecognized protocol format. Try copying again.", "فرمت پروتکل شناسایی نشد. مجدداً کپی کنید."),
        "latency" to Pair("Latency", "تأخیر شبکه"),
        "active_connection" to Pair("Active Protocol", "پروتکل فعال"),
        "location" to Pair("Server Location", "موقعیت جغرافیایی"),
        "test_ping_all" to Pair("Test all connections latency", "سنجش پینگ تمام اتصالات"),
        "testing_ping_btn" to Pair("Ping Testing...", "در حال سنجش..."),
        "delete_all" to Pair("Remove All Profile", "حذف تمام پروفایل‌ها"),
        "import_hint" to Pair("Accepts: VLESS, VMESS, Trojan, Shadowsocks, Socks, HTTP, WireGuard", "پشتیبانی از: VLESS, VMESS, Trojan, Shadowsocks, Socks, HTTP, WireGuard"),

        // Stats Screen
        "traffic_stats" to Pair("Traffic Volume & Data Analytics", "آمار ترافیک و دانلود"),
        "total_volume" to Pair("Total Volume Used", "کل حجم مصرف شده"),
        "downloads" to Pair("Downloaded Volume", "حجم دانلود شده"),
        "uploads" to Pair("Uploaded Volume", "حجم آپلود شده"),
        "recent_history" to Pair("Recent Connection History", "تاریخچه اتصالات اخیر"),
        "no_history" to Pair("No connection history has been registered yet.", "هیچ تاریخچه اتصالی ثبت نشده است."),
        "traffic" to Pair("Traffic usage on this period", "میزان ترافیک مصرفی در این دوره"),
        "total_combined" to Pair("Total usage", "کل مصرف"),
        "history" to Pair("Recent Connection History Logs", "تاریخچه اتصال اخیر"),
        
        // Settings Screen
        "app_settings" to Pair("App Settings", "تنظیمات برنامه"),
        "general_settings" to Pair("General Settings", "تنظیمات عمومی"),
        "premium_acc" to Pair("Premium Account Status", "وضعیت حساب کاربری"),
        "premium_active" to Pair("Premium Active", "پرمیوم بی‌نهایت فعال"),
        "lang_label" to Pair("App Language", "زبان برنامه / Language"),
        "lang_desc" to Pair("Toggle between Persian and English layouts", "تغییر زبان بین انگلیسی و فارسی"),
        "dark_mode" to Pair("Dark Theme (System default)", "حالت تاریک (Dark Mode)"),
        "dark_desc" to Pair("Enable elegant protective interface theme", "تم پیش‌فرض برنامه روی دارک مود"),
        "notifications" to Pair("App Notifications", "اعلان‌های برنامه"),
        "notif_desc" to Pair("Show connection timer and speed badge", "نمایش اعلان زمان و سرعت اتصال فعال"),
        "auto_connect" to Pair("Auto-Connect on Launch", "اتصال خودکار به محض ورود"),
        "autoconn_desc" to Pair("Connect automatically to fastest latency server", "اتصال اتوماتیک به سریع‌ترین پروکسی"),
        "about_section" to Pair("About ProtectoNG", "درباره برنامه"),
        "about_btn" to Pair("About Program Details", "درباره ProtectoNG"),
        "about_desc" to Pair("View official communication channel and details", "مشاهده کانال ارتباطی دپارتمان توسعه‌دهنده"),
        "about" to Pair("About ProtectoNG", "درباره ProtectoNG"),
        "about_msg" to Pair(
            "This software is engineered in native Jetpack Compose representing the ultimate high-fidelity UX simulator, and does not perform active connection routing or dynamic system tunnel protocols.",
            "این برنامه بر اساس استانداردهای مدرن با هدف تست رابط کاربری ناتیو پیاده‌سازی شده است و فاقد هرگونه کد هسته شبکه یا تونل واقعی می‌باشد."
        ),
        "account_status" to Pair("Account Status", "وضعیت حساب"),
        "premium" to Pair("Active Premium", "پرمیوم بی‌نهایت فعال"),
        "dev_by" to Pair("Developed by ProtectoNG Core Team", "توسعه یافته توسط دپارتمان فنی"),
        "close_btn" to Pair("Understood & Close", "شناخت و بستن"),
        "about_dialog_desc" to Pair(
            "ProtectoNG is a modern high-fidelity configuration manager app, optimized for parsing protocols like VLESS, VMESS, Shadowsocks, Trojan, and Socks with local storage support.",
            "پروتکتو ان‌جی برنامه‌ای پیشرفته جهت مدیریت کانفیگ‌ها و پشتیبانی از پروتکل‌های شبکه نظیر VLESS, VMESS, Trojan و Socks است."
        ),

        // Help Desk & Support
        "help_desk" to Pair("Help Desk & Support", "پشتیبانی و راهنما"),
        "support_btn" to Pair("Contact Support Team", "تماس با تیم پشتیبانی"),
        "support_desc" to Pair("Ask questions or report proxy connection issues", "ارسال پیام به کارشناسان و رفع مشکل"),
        "diagnostic_btn" to Pair("Network Ping & Diagnostics", "تست پینگ و خطایابی شبکه"),
        "diagnostic_desc" to Pair("Run diagnostics to verify routing and handshakes", "خطایابی هوشمند تونل و پینگ پروتکل‌ها"),
        "diagnostics_title" to Pair("Diagnostics & Ping Test", "خطایابی و تست پینگ"),
        "diagnostics_test_ping" to Pair("Test Ping to active server", "تست پینگ سرور فعلی"),
        "diagnostics_test_github" to Pair("Verify GitHub response time", "تست سرعت دسترسی به گیت‌هاب"),
        "diagnostics_test_tunnel" to Pair("Check tunneling protocol handshakes", "بررسی پروتکل تونل زنی"),
        "run_test_btn" to Pair("Run Diagnostics", "شروع تست"),
        "diagnostics_running" to Pair("Running diagnostics...", "در حال اجرای خطایابی..."),

        // Empty state
        "no_configurations_added" to Pair("No configurations added", "هیچ پیکربندی اضافه نشده است"),
        "empty_configs_hint" to Pair("Please import secure VLESS, VMESS, Trojan, Shadowsocks, Socks or HTTP proxy, or load JSON config files to connect safely.", "لطفاً برای اتصال امن، یک آدرس یا فایل کانفیگ وارد کنید.")
    )

    fun t(key: String, language: String): String {
        val strings = translations[key] ?: return key
        return if (language == "English") strings.first else strings.second
    }

    fun localizeHistoryTime(timeStr: String, language: String): String {
        if (language == "English") {
            return timeStr
                .replace("هم‌اکنون", "Just now")
                .replace("دیروز", "Yesterday")
                .replace("روز پیش", "days ago")
                .replace("ساعت پیش", "hours ago")
        }
        return timeStr
    }
}
