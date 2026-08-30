using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.Primitives;
using System.Windows.Data;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Shapes;
using System.Windows.Threading;

namespace PrivaFinDesktop
{
    // ==========================================
    // DATA MODELS (C# 5 Compatible)
    // ==========================================
    public class FinanceItem
    {
        public int id { get; set; }
        public string title { get; set; }
        public double amount { get; set; }
        public string type { get; set; } // SALARY, INVESTMENT, BOX, LENT, BILL, APARTMENT, CHALLENGE
        public string category { get; set; }
        public long date { get; set; }
        public string description { get; set; }
        public bool isCompleted { get; set; }
        public double targetAmount { get; set; }
        public long? dueDate { get; set; }

        public FinanceItem()
        {
            title = "";
            type = "APARTMENT";
            category = "Geral";
            date = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            description = "";
            isCompleted = false;
            targetAmount = 0.0;
        }
    }

    public class SyncPayload
    {
        public string deviceName { get; set; }
        public long timestamp { get; set; }
        public List<FinanceItem> items { get; set; }
        public List<string> apartmentSubcategories { get; set; }

        public SyncPayload()
        {
            deviceName = "Windows PC";
            timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            items = new List<FinanceItem>();
            apartmentSubcategories = new List<string>();
        }
    }

    public class SyncResponse
    {
        public bool success { get; set; }
        public string message { get; set; }
        public List<FinanceItem> serverItems { get; set; }
        public List<string> serverApartmentSubcategories { get; set; }

        public SyncResponse()
        {
            message = "";
            serverItems = new List<FinanceItem>();
            serverApartmentSubcategories = new List<string>();
        }
    }

    public class AppState
    {
        public List<FinanceItem> items { get; set; }
        public List<string> apartmentSubcategories { get; set; }
        public double challengeMultiplier { get; set; }

        public AppState()
        {
            items = new List<FinanceItem>();
            apartmentSubcategories = new List<string>
            {
                "Aluguel / Financiamento", "Condomínio", "Energia Elétrica", "Água e Esgoto",
                "Internet / Wi-Fi", "Gás", "IPTU", "Manutenção e Reformas", "Móveis e Eletros", "Limpeza e Casa"
            };
            challengeMultiplier = 5.0;
        }
    }

    // ==========================================
    // STORAGE HELPER
    // ==========================================
    public static class StorageHelper
    {
        private static readonly string AppDataFolder = System.IO.Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "PrivaFin");
        private static readonly string DataFilePath = System.IO.Path.Combine(AppDataFolder, "privafin_data.json");

        public static AppState LoadState()
        {
            try
            {
                if (!Directory.Exists(AppDataFolder))
                    Directory.CreateDirectory(AppDataFolder);

                if (File.Exists(DataFilePath))
                {
                    string json = File.ReadAllText(DataFilePath, Encoding.UTF8);
                    var state = SimpleJson.Deserialize<AppState>(json);
                    if (state != null)
                    {
                        if (state.apartmentSubcategories == null || state.apartmentSubcategories.Count == 0)
                        {
                            state.apartmentSubcategories = new List<string>
                            {
                                "Aluguel / Financiamento", "Condomínio", "Energia Elétrica", "Água e Esgoto",
                                "Internet / Wi-Fi", "Gás", "IPTU", "Manutenção e Reformas", "Móveis e Eletros", "Limpeza e Casa"
                            };
                        }
                        return state;
                    }
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine("Error loading data: " + ex.Message);
            }
            return new AppState();
        }

        public static void SaveState(AppState state)
        {
            try
            {
                if (!Directory.Exists(AppDataFolder))
                    Directory.CreateDirectory(AppDataFolder);

                string json = SimpleJson.Serialize(state);
                File.WriteAllText(DataFilePath, json, Encoding.UTF8);
            }
            catch (Exception ex)
            {
                Console.WriteLine("Error saving data: " + ex.Message);
            }
        }
    }

    // ==========================================
    // LIGHTWEIGHT JSON SERIALIZER (C# 5 Compatible)
    // ==========================================
    public static class SimpleJson
    {
        public static string Serialize(object obj)
        {
            return SerializeValue(obj);
        }

        private static string SerializeValue(object val)
        {
            if (val == null) return "null";
            if (val is string) return "\"" + EscapeString((string)val) + "\"";
            if (val is bool) return ((bool)val) ? "true" : "false";
            if (val is int || val is long || val is double || val is float)
            {
                return Convert.ToString(val, System.Globalization.CultureInfo.InvariantCulture);
            }
            if (val is System.Collections.IEnumerable && !(val is string))
            {
                var sb = new StringBuilder("[");
                bool first = true;
                foreach (var item in (System.Collections.IEnumerable)val)
                {
                    if (!first) sb.Append(",");
                    sb.Append(SerializeValue(item));
                    first = false;
                }
                sb.Append("]");
                return sb.ToString();
            }

            // Object
            var type = val.GetType();
            var props = type.GetProperties();
            var objSb = new StringBuilder("{");
            bool firstProp = true;
            foreach (var p in props)
            {
                if (!firstProp) objSb.Append(",");
                objSb.Append("\"").Append(p.Name).Append("\":");
                objSb.Append(SerializeValue(p.GetValue(val, null)));
                firstProp = false;
            }
            objSb.Append("}");
            return objSb.ToString();
        }

        private static string EscapeString(string s)
        {
            return s.Replace("\\", "\\\\")
                    .Replace("\"", "\\\"")
                    .Replace("\r", "\\r")
                    .Replace("\n", "\\n")
                    .Replace("\t", "\\t");
        }

        public static T Deserialize<T>(string json) where T : new()
        {
            try
            {
                var dict = ParseObject(json.Trim());
                var res = new T();
                PopulateObject(res, dict);
                return res;
            }
            catch
            {
                return new T();
            }
        }

        private static void PopulateObject(object target, Dictionary<string, object> dict)
        {
            var props = target.GetType().GetProperties();
            foreach (var p in props)
            {
                if (dict.ContainsKey(p.Name))
                {
                    var val = dict[p.Name];
                    if (val != null)
                    {
                        if (p.PropertyType == typeof(string)) p.SetValue(target, val.ToString(), null);
                        else if (p.PropertyType == typeof(int)) p.SetValue(target, Convert.ToInt32(val), null);
                        else if (p.PropertyType == typeof(long)) p.SetValue(target, Convert.ToInt64(val), null);
                        else if (p.PropertyType == typeof(long?)) p.SetValue(target, (long?)Convert.ToInt64(val), null);
                        else if (p.PropertyType == typeof(double)) p.SetValue(target, Convert.ToDouble(val, System.Globalization.CultureInfo.InvariantCulture), null);
                        else if (p.PropertyType == typeof(bool)) p.SetValue(target, Convert.ToBoolean(val), null);
                        else if (p.PropertyType == typeof(List<string>) && val is List<object>)
                        {
                            var sList = (List<object>)val;
                            p.SetValue(target, sList.Select(x => x.ToString()).ToList(), null);
                        }
                        else if (p.PropertyType == typeof(List<FinanceItem>) && val is List<object>)
                        {
                            var iList = (List<object>)val;
                            var fList = new List<FinanceItem>();
                            foreach (var o in iList)
                            {
                                if (o is Dictionary<string, object>)
                                {
                                    var fi = new FinanceItem();
                                    PopulateObject(fi, (Dictionary<string, object>)o);
                                    fList.Add(fi);
                                }
                            }
                            p.SetValue(target, fList, null);
                        }
                    }
                }
            }
        }

        private static Dictionary<string, object> ParseObject(string json)
        {
            var dict = new Dictionary<string, object>(StringComparer.OrdinalIgnoreCase);
            if (!json.StartsWith("{") || !json.EndsWith("}")) return dict;
            int idx = 1;
            int len = json.Length - 1;

            while (idx < len)
            {
                SkipWhitespace(json, ref idx);
                if (idx >= len) break;
                if (json[idx] == ',') { idx++; continue; }

                string key = ParseString(json, ref idx);
                SkipWhitespace(json, ref idx);
                if (idx < len && json[idx] == ':') idx++;
                SkipWhitespace(json, ref idx);
                object val = ParseValue(json, ref idx);
                dict[key] = val;
            }
            return dict;
        }

        private static object ParseValue(string json, ref int idx)
        {
            SkipWhitespace(json, ref idx);
            if (idx >= json.Length) return null;
            char c = json[idx];
            if (c == '"') return ParseString(json, ref idx);
            if (c == '{')
            {
                int start = idx;
                int depth = 0;
                while (idx < json.Length)
                {
                    if (json[idx] == '{') depth++;
                    else if (json[idx] == '}') { depth--; if (depth == 0) { idx++; break; } }
                    idx++;
                }
                return ParseObject(json.Substring(start, idx - start));
            }
            if (c == '[')
            {
                idx++; // skip [
                var list = new List<object>();
                while (idx < json.Length)
                {
                    SkipWhitespace(json, ref idx);
                    if (idx < json.Length && json[idx] == ']') { idx++; break; }
                    if (json[idx] == ',') { idx++; continue; }
                    list.Add(ParseValue(json, ref idx));
                }
                return list;
            }
            if (char.IsDigit(c) || c == '-')
            {
                int start = idx;
                while (idx < json.Length && (char.IsDigit(json[idx]) || json[idx] == '.' || json[idx] == '-')) idx++;
                string numStr = json.Substring(start, idx - start);
                if (numStr.Contains(".")) return double.Parse(numStr, System.Globalization.CultureInfo.InvariantCulture);
                return long.Parse(numStr);
            }
            if (json.Substring(idx).StartsWith("true", StringComparison.OrdinalIgnoreCase)) { idx += 4; return true; }
            if (json.Substring(idx).StartsWith("false", StringComparison.OrdinalIgnoreCase)) { idx += 5; return false; }
            if (json.Substring(idx).StartsWith("null", StringComparison.OrdinalIgnoreCase)) { idx += 4; return null; }
            idx++;
            return null;
        }

        private static string ParseString(string json, ref int idx)
        {
            if (json[idx] == '"') idx++;
            var sb = new StringBuilder();
            while (idx < json.Length)
            {
                char c = json[idx++];
                if (c == '\\')
                {
                    if (idx < json.Length)
                    {
                        char esc = json[idx++];
                        if (esc == 'n') sb.Append('\n');
                        else if (esc == 'r') sb.Append('\r');
                        else if (esc == 't') sb.Append('\t');
                        else sb.Append(esc);
                    }
                }
                else if (c == '"') break;
                else sb.Append(c);
            }
            return sb.ToString();
        }

        private static void SkipWhitespace(string json, ref int idx)
        {
            while (idx < json.Length && char.IsWhiteSpace(json[idx])) idx++;
        }
    }

    // ==========================================
    // MAIN APPLICATION WINDOW
    // ==========================================
    public class MainWindow : Window
    {
        private AppState state;
        private int currentTabIndex = 0; // 0: Resumo, 1: Transações, 2: Caixinhas, 3: Desafio 52S, 4: Empréstimos, 5: Sincronização

        // UI Root Elements
        private Grid mainGrid;
        private StackPanel sidebarPanel;
        private ContentControl mainContent;

        // Colors
        private readonly SolidColorBrush BgDark = new SolidColorBrush(Color.FromRgb(12, 16, 23));
        private readonly SolidColorBrush CardDark = new SolidColorBrush(Color.FromRgb(22, 31, 43));
        private readonly SolidColorBrush CardAltDark = new SolidColorBrush(Color.FromRgb(31, 43, 62));
        private readonly SolidColorBrush EmeraldGreen = new SolidColorBrush(Color.FromRgb(0, 230, 118));
        private readonly SolidColorBrush OceanBlue = new SolidColorBrush(Color.FromRgb(41, 182, 246));
        private readonly SolidColorBrush GoldAmber = new SolidColorBrush(Color.FromRgb(255, 167, 38));
        private readonly SolidColorBrush LavenderPurple = new SolidColorBrush(Color.FromRgb(171, 71, 188));
        private readonly SolidColorBrush CoralRed = new SolidColorBrush(Color.FromRgb(239, 83, 80));
        private readonly SolidColorBrush ApartmentTeal = new SolidColorBrush(Color.FromRgb(38, 198, 218));
        private readonly SolidColorBrush TextWhite = new SolidColorBrush(Color.FromRgb(243, 244, 246));
        private readonly SolidColorBrush TextMuted = new SolidColorBrush(Color.FromRgb(156, 163, 175));
        private readonly SolidColorBrush BorderMuted = new SolidColorBrush(Color.FromRgb(45, 61, 84));

        // Challenge selection state
        private HashSet<int> selectedChallengeWeekIds = new HashSet<int>();
        private Border floatingChallengePopup = null;

        public MainWindow()
        {
            Title = "PrivaFin - Finanças Pessoais (Windows Desktop)";
            Width = 1150;
            Height = 780;
            MinWidth = 950;
            MinHeight = 650;
            WindowStartupLocation = WindowStartupLocation.CenterScreen;
            Background = BgDark;
            Foreground = TextWhite;
            FontFamily = new FontFamily("Segoe UI, Arial");

            state = StorageHelper.LoadState();

            BuildLayout();
            NavigateTo(0);
        }

        private void BuildLayout()
        {
            mainGrid = new Grid();
            mainGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(240) });
            mainGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

            // Sidebar
            var sidebarBorder = new Border
            {
                Background = CardDark,
                BorderBrush = BorderMuted,
                BorderThickness = new Thickness(0, 0, 1, 0)
            };

            sidebarPanel = new StackPanel { Margin = new Thickness(16) };

            // Brand Header
            var brandRow = new StackPanel { Orientation = Orientation.Horizontal, Margin = new Thickness(0, 8, 0, 24) };
            var logoIcon = new Border
            {
                Width = 36,
                Height = 36,
                CornerRadius = new CornerRadius(18),
                Background = new SolidColorBrush(Color.FromArgb(40, 0, 230, 118)),
                Child = new TextBlock
                {
                    Text = "🔒",
                    FontSize = 18,
                    HorizontalAlignment = HorizontalAlignment.Center,
                    VerticalAlignment = VerticalAlignment.Center
                }
            };
            var brandText = new StackPanel { Margin = new Thickness(10, 0, 0, 0) };
            brandText.Children.Add(new TextBlock { Text = "PrivaFin", FontSize = 18, FontWeight = FontWeights.Bold, Foreground = TextWhite });
            brandText.Children.Add(new TextBlock { Text = "Windows Desktop", FontSize = 11, Foreground = TextMuted });
            brandRow.Children.Add(logoIcon);
            brandRow.Children.Add(brandText);
            sidebarPanel.Children.Add(brandRow);

            // Nav items
            sidebarPanel.Children.Add(CreateNavButton("📊 Resumo Geral", 0));
            sidebarPanel.Children.Add(CreateNavButton("💳 Transações", 1));
            sidebarPanel.Children.Add(CreateNavButton("🎁 Caixinhas & Metas", 2));
            sidebarPanel.Children.Add(CreateNavButton("🏆 Desafio 52 Semanas", 3));
            sidebarPanel.Children.Add(CreateNavButton("🤝 Empréstimos", 4));
            sidebarPanel.Children.Add(new Separator { Background = BorderMuted, Margin = new Thickness(0, 16, 0, 16) });
            sidebarPanel.Children.Add(CreateNavButton("🔄 Sincronizar com Android", 5, OceanBlue));

            sidebarBorder.Child = sidebarPanel;
            Grid.SetColumn(sidebarBorder, 0);
            mainGrid.Children.Add(sidebarBorder);

            // Main Content Area
            mainContent = new ContentControl { Margin = new Thickness(24) };
            Grid.SetColumn(mainContent, 1);
            mainGrid.Children.Add(mainContent);

            Content = mainGrid;
        }

        private Button CreateNavButton(string label, int index, SolidColorBrush activeColor = null)
        {
            var btn = new Button
            {
                Content = label,
                Height = 44,
                Margin = new Thickness(0, 3, 0, 3),
                FontSize = 14,
                FontWeight = FontWeights.SemiBold,
                HorizontalContentAlignment = HorizontalAlignment.Left,
                Padding = new Thickness(16, 0, 0, 0),
                Cursor = Cursors.Hand,
                Background = currentTabIndex == index ? (activeColor ?? EmeraldGreen) : Brushes.Transparent,
                Foreground = currentTabIndex == index ? Brushes.Black : TextWhite,
                BorderThickness = new Thickness(0),
                Tag = index
            };

            btn.Click += delegate(object s, RoutedEventArgs e) { NavigateTo(index); };
            return btn;
        }

        private void NavigateTo(int index)
        {
            currentTabIndex = index;
            selectedChallengeWeekIds.Clear();

            // Refresh sidebar button states
            foreach (var child in sidebarPanel.Children)
            {
                if (child is Button)
                {
                    var b = (Button)child;
                    if (b.Tag is int)
                    {
                        int idx = (int)b.Tag;
                        b.Background = (idx == currentTabIndex) ? (idx == 5 ? OceanBlue : EmeraldGreen) : Brushes.Transparent;
                        b.Foreground = (idx == currentTabIndex) ? Brushes.Black : TextWhite;
                    }
                }
            }

            switch (index)
            {
                case 0: mainContent.Content = BuildDashboardView(); break;
                case 1: mainContent.Content = BuildTransactionsView(); break;
                case 2: mainContent.Content = BuildBoxesView(); break;
                case 3: mainContent.Content = BuildChallengeView(); break;
                case 4: mainContent.Content = BuildLentView(); break;
                case 5: mainContent.Content = BuildSyncView(); break;
            }
        }

        // ==========================================
        // VIEW: DASHBOARD
        // ==========================================
        private UIElement BuildDashboardView()
        {
            var scroll = new ScrollViewer { VerticalScrollBarVisibility = ScrollBarVisibility.Auto };
            var panel = new StackPanel { Margin = new Thickness(0, 0, 16, 16) };

            // Header
            panel.Children.Add(new TextBlock { Text = "Painel Financeiro", FontSize = 26, FontWeight = FontWeights.Bold, Foreground = TextWhite });
            panel.Children.Add(new TextBlock { Text = "Resumo e saúde das suas finanças sincronizadas", FontSize = 13, Foreground = TextMuted, Margin = new Thickness(0, 2, 0, 20) });

            // Metrics
            double totalSalary = state.items.Where(i => i.type == "SALARY").Sum(i => i.amount);
            double totalInvested = state.items.Where(i => i.type == "INVESTMENT" || i.type == "BOX").Sum(i => i.amount);
            double totalApartment = state.items.Where(i => i.type == "APARTMENT").Sum(i => i.amount);
            double activeLent = state.items.Where(i => i.type == "LENT" && !i.isCompleted).Sum(i => i.amount);

            // Hero Metric Card
            var heroCard = new Border
            {
                Background = CardDark,
                BorderBrush = BorderMuted,
                BorderThickness = new Thickness(1),
                CornerRadius = new CornerRadius(16),
                Padding = new Thickness(24),
                Margin = new Thickness(0, 0, 0, 20)
            };
            var heroGrid = new Grid();
            heroGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            heroGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            heroGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

            heroGrid.Children.Add(CreateStatBlock("TOTAL INVESTIDO / POUPADO", "R$ " + totalInvested.ToString("N2"), EmeraldGreen, 0));
            heroGrid.Children.Add(CreateStatBlock("GASTOS COM APARTAMENTO", "R$ " + totalApartment.ToString("N2"), ApartmentTeal, 1));
            heroGrid.Children.Add(CreateStatBlock("A RECEBER (EMPRÉSTIMOS)", "R$ " + activeLent.ToString("N2"), OceanBlue, 2));

            heroCard.Child = heroGrid;
            panel.Children.Add(heroCard);

            // Quick Hubs Section
            panel.Children.Add(new TextBlock { Text = "Acesso Rápido aos Módulos", FontSize = 18, FontWeight = FontWeights.Bold, Margin = new Thickness(0, 10, 0, 12) });
            var hubGrid = new Grid { Margin = new Thickness(0, 0, 0, 24) };
            hubGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            hubGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            hubGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            hubGrid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });

            hubGrid.Children.Add(CreateHubCard("🏢 Apartamento & Moradia", "R$ " + totalApartment.ToString("N2"), ApartmentTeal, 1, 0));
            hubGrid.Children.Add(CreateHubCard("🎁 Caixinhas & Metas", state.items.Count(i => i.type == "BOX") + " criadas", GoldAmber, 2, 1));
            hubGrid.Children.Add(CreateHubCard("🏆 Desafio 52 Semanas", "Depósitos progressivos", EmeraldGreen, 3, 2));

            panel.Children.Add(hubGrid);

            // Recent Transactions Table
            panel.Children.Add(new TextBlock { Text = "Transações Recentes", FontSize = 18, FontWeight = FontWeights.Bold, Margin = new Thickness(0, 10, 0, 12) });
            var tableCard = new Border
            {
                Background = CardDark,
                BorderBrush = BorderMuted,
                BorderThickness = new Thickness(1),
                CornerRadius = new CornerRadius(14),
                Padding = new Thickness(16)
            };

            var recentItems = state.items.OrderByDescending(i => i.date).Take(6).ToList();
            if (recentItems.Count == 0)
            {
                tableCard.Child = new TextBlock
                {
                    Text = "Nenhuma transação registrada ainda.",
                    Foreground = TextMuted,
                    HorizontalAlignment = HorizontalAlignment.Center,
                    Margin = new Thickness(20)
                };
            }
            else
            {
                var listStack = new StackPanel();
                foreach (var item in recentItems)
                {
                    listStack.Children.Add(CreateTransactionRow(item));
                }
                tableCard.Child = listStack;
            }
            panel.Children.Add(tableCard);

            scroll.Content = panel;
            return scroll;
        }

        private UIElement CreateStatBlock(string label, string value, SolidColorBrush color, int col)
        {
            var sp = new StackPanel { Margin = new Thickness(8) };
            sp.Children.Add(new TextBlock { Text = label, FontSize = 11, FontWeight = FontWeights.SemiBold, Foreground = TextMuted, Margin = new Thickness(0, 0, 0, 4) });
            sp.Children.Add(new TextBlock { Text = value, FontSize = 22, FontWeight = FontWeights.Bold, Foreground = color });
            Grid.SetColumn(sp, col);
            return sp;
        }

        private UIElement CreateHubCard(string title, string subtitle, SolidColorBrush color, int tabTarget, int col)
        {
            var card = new Border
            {
                Background = CardDark,
                BorderBrush = BorderMuted,
                BorderThickness = new Thickness(1),
                CornerRadius = new CornerRadius(12),
                Padding = new Thickness(16),
                Margin = new Thickness(col == 0 ? 0 : 8, 0, col == 2 ? 0 : 8, 0),
                Cursor = Cursors.Hand
            };
            var sp = new StackPanel();
            sp.Children.Add(new TextBlock { Text = title, FontSize = 15, FontWeight = FontWeights.Bold, Foreground = color });
            sp.Children.Add(new TextBlock { Text = subtitle, FontSize = 13, Foreground = TextMuted, Margin = new Thickness(0, 4, 0, 0) });
            card.Child = sp;
            card.MouseDown += delegate(object s, MouseButtonEventArgs e) { NavigateTo(tabTarget); };
            Grid.SetColumn(card, col);
            return card;
        }

        private UIElement CreateTransactionRow(FinanceItem item)
        {
            var row = new Border
            {
                Background = CardAltDark,
                CornerRadius = new CornerRadius(8),
                Padding = new Thickness(12, 10, 12, 10),
                Margin = new Thickness(0, 4, 0, 4)
            };
            var grid = new Grid();
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(2, GridUnitType.Star) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1.5, GridUnitType.Star) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

            var titleSp = new StackPanel();
            titleSp.Children.Add(new TextBlock { Text = item.title, FontWeight = FontWeights.Bold, Foreground = TextWhite });
            titleSp.Children.Add(new TextBlock { Text = DateTimeOffset.FromUnixTimeMilliseconds(item.date).ToString("dd/MM/yyyy"), FontSize = 11, Foreground = TextMuted });
            Grid.SetColumn(titleSp, 0);

            var catText = new TextBlock
            {
                Text = item.category,
                Foreground = GetTypeColor(item.type),
                VerticalAlignment = VerticalAlignment.Center,
                FontWeight = FontWeights.SemiBold
            };
            Grid.SetColumn(catText, 1);

            bool isIncome = item.type == "SALARY" || (item.type == "LENT" && item.isCompleted);
            var amtText = new TextBlock
            {
                Text = (isIncome ? "+ " : "- ") + "R$ " + item.amount.ToString("N2"),
                Foreground = isIncome ? EmeraldGreen : TextWhite,
                FontWeight = FontWeights.Bold,
                VerticalAlignment = VerticalAlignment.Center,
                HorizontalAlignment = HorizontalAlignment.Right
            };
            Grid.SetColumn(amtText, 2);

            var deleteBtn = new Button
            {
                Content = "✕",
                Width = 28,
                Height = 28,
                Background = Brushes.Transparent,
                Foreground = TextMuted,
                BorderThickness = new Thickness(0),
                Cursor = Cursors.Hand,
                HorizontalAlignment = HorizontalAlignment.Right
            };
            deleteBtn.Click += delegate(object s, RoutedEventArgs e)
            {
                state.items.Remove(item);
                StorageHelper.SaveState(state);
                NavigateTo(currentTabIndex);
            };
            Grid.SetColumn(deleteBtn, 3);

            grid.Children.Add(titleSp);
            grid.Children.Add(catText);
            grid.Children.Add(amtText);
            grid.Children.Add(deleteBtn);

            row.Child = grid;
            return row;
        }

        private SolidColorBrush GetTypeColor(string type)
        {
            switch (type)
            {
                case "APARTMENT": return ApartmentTeal;
                case "SALARY": return EmeraldGreen;
                case "INVESTMENT": return OceanBlue;
                case "BOX": return GoldAmber;
                case "LENT": return LavenderPurple;
                default: return CoralRed;
            }
        }

        // ==========================================
        // VIEW: TRANSAÇÕES
        // ==========================================
        private UIElement BuildTransactionsView()
        {
            var scroll = new ScrollViewer { VerticalScrollBarVisibility = ScrollBarVisibility.Auto };
            var panel = new StackPanel { Margin = new Thickness(0, 0, 16, 16) };

            var topRow = new Grid { Margin = new Thickness(0, 0, 0, 16) };
            topRow.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            topRow.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });

            var titleSp = new StackPanel();
            titleSp.Children.Add(new TextBlock { Text = "Transações", FontSize = 26, FontWeight = FontWeights.Bold, Foreground = TextWhite });
            titleSp.Children.Add(new TextBlock { Text = "Controle de receitas, despesas e moradia", FontSize = 13, Foreground = TextMuted });
            Grid.SetColumn(titleSp, 0);

            var addBtn = new Button
            {
                Content = "➕ Nova Transação",
                Background = EmeraldGreen,
                Foreground = Brushes.Black,
                FontWeight = FontWeights.Bold,
                Padding = new Thickness(16, 10, 16, 10),
                Cursor = Cursors.Hand
            };
            addBtn.Click += delegate(object s, RoutedEventArgs e) { ShowAddTransactionModal(); };
            Grid.SetColumn(addBtn, 1);

            topRow.Children.Add(titleSp);
            topRow.Children.Add(addBtn);
            panel.Children.Add(topRow);

            // Filter Buttons Row
            var filtersPanel = new WrapPanel { Margin = new Thickness(0, 0, 0, 16) };
            string[] filters = new string[] { "TUDO", "APARTMENT", "SALARY", "INVESTMENT", "BOX", "LENT", "BILL" };
            string[] labels = new string[] { "Tudo", "🏢 Apartamento", "💵 Salário", "📈 Investimentos", "🎁 Caixinhas", "🤝 Emprestados", "🧾 Contas" };

            for (int i = 0; i < filters.Length; i++)
            {
                string f = filters[i];
                var fBtn = new Button
                {
                    Content = labels[i],
                    Margin = new Thickness(0, 0, 8, 8),
                    Padding = new Thickness(14, 6, 14, 6),
                    Background = CardAltDark,
                    Foreground = TextWhite,
                    BorderBrush = BorderMuted,
                    BorderThickness = new Thickness(1),
                    Cursor = Cursors.Hand
                };
                fBtn.Click += delegate(object s, RoutedEventArgs e)
                {
                    var filtered = (f == "TUDO") ? state.items : state.items.Where(it => it.type == f).ToList();
                    RenderTransactionList(panel, filtered);
                };
                filtersPanel.Children.Add(fBtn);
            }
            panel.Children.Add(filtersPanel);

            // Transaction items container
            var listContainer = new StackPanel();
            panel.Children.Add(listContainer);
            RenderTransactionList(panel, state.items);

            scroll.Content = panel;
            return scroll;
        }

        private void RenderTransactionList(StackPanel parent, List<FinanceItem> list)
        {
            if (parent.Children.Count > 3) parent.Children.RemoveAt(parent.Children.Count - 1);

            var card = new Border
            {
                Background = CardDark,
                BorderBrush = BorderMuted,
                BorderThickness = new Thickness(1),
                CornerRadius = new CornerRadius(14),
                Padding = new Thickness(16)
            };

            var itemsStack = new StackPanel();
            var sorted = list.OrderByDescending(i => i.date).ToList();

            if (sorted.Count == 0)
            {
                itemsStack.Children.Add(new TextBlock
                {
                    Text = "Nenhuma transação encontrada.",
                    Foreground = TextMuted,
                    HorizontalAlignment = HorizontalAlignment.Center,
                    Margin = new Thickness(20)
                });
            }
            else
            {
                foreach (var item in sorted)
                {
                    itemsStack.Children.Add(CreateTransactionRow(item));
                }
            }
            card.Child = itemsStack;
            parent.Children.Add(card);
        }

        private void ShowAddTransactionModal()
        {
            var win = new Window
            {
                Title = "Nova Transação",
                Width = 520,
                Height = 580,
                WindowStartupLocation = WindowStartupLocation.CenterOwner,
                Owner = this,
                Background = CardDark,
                Foreground = TextWhite,
                FontFamily = new FontFamily("Segoe UI")
            };

            var sp = new StackPanel { Margin = new Thickness(24) };
            sp.Children.Add(new TextBlock { Text = "Nova Transação", FontSize = 20, FontWeight = FontWeights.Bold, Margin = new Thickness(0, 0, 0, 16) });

            // Type ComboBox
            sp.Children.Add(new TextBlock { Text = "Tipo de Registro:", Foreground = TextMuted, Margin = new Thickness(0, 0, 0, 4) });
            var typeCombo = new ComboBox { Height = 36, Margin = new Thickness(0, 0, 0, 12) };
            typeCombo.Items.Add("Apartamento (APARTMENT)");
            typeCombo.Items.Add("Salário / Renda (SALARY)");
            typeCombo.Items.Add("Investimento (INVESTMENT)");
            typeCombo.Items.Add("Caixinha (BOX)");
            typeCombo.Items.Add("Emprestado (LENT)");
            typeCombo.Items.Add("Conta / Despesa (BILL)");
            typeCombo.SelectedIndex = 0;
            sp.Children.Add(typeCombo);

            // Subcategory Selection (for Apartment)
            var subcatLabel = new TextBlock { Text = "Subcategoria do Apartamento:", Foreground = TextMuted, Margin = new Thickness(0, 0, 0, 4) };
            var subcatCombo = new ComboBox { Height = 36, Margin = new Thickness(0, 0, 0, 12) };
            foreach (var sub in state.apartmentSubcategories) subcatCombo.Items.Add(sub);
            if (subcatCombo.Items.Count > 0) subcatCombo.SelectedIndex = 0;
            sp.Children.Add(subcatLabel);
            sp.Children.Add(subcatCombo);

            typeCombo.SelectionChanged += delegate(object s, SelectionChangedEventArgs e)
            {
                bool isApartment = typeCombo.SelectedIndex == 0;
                subcatLabel.Visibility = isApartment ? Visibility.Visible : Visibility.Collapsed;
                subcatCombo.Visibility = isApartment ? Visibility.Visible : Visibility.Collapsed;
            };

            // Title
            sp.Children.Add(new TextBlock { Text = "Título / Descrição:", Foreground = TextMuted, Margin = new Thickness(0, 0, 0, 4) });
            var titleBox = new TextBox { Height = 36, Margin = new Thickness(0, 0, 0, 12), Background = CardAltDark, Foreground = TextWhite, BorderBrush = BorderMuted };
            sp.Children.Add(titleBox);

            // Amount
            sp.Children.Add(new TextBlock { Text = "Valor (R$):", Foreground = TextMuted, Margin = new Thickness(0, 0, 0, 4) });
            var amountBox = new TextBox { Height = 36, Margin = new Thickness(0, 0, 0, 12), Background = CardAltDark, Foreground = TextWhite, BorderBrush = BorderMuted };
            sp.Children.Add(amountBox);

            // Save Button
            var saveBtn = new Button
            {
                Content = "Salvar Registro",
                Background = EmeraldGreen,
                Foreground = Brushes.Black,
                FontWeight = FontWeights.Bold,
                Height = 42,
                Margin = new Thickness(0, 16, 0, 0),
                Cursor = Cursors.Hand
            };
            saveBtn.Click += delegate(object s, RoutedEventArgs e)
            {
                double amt;
                if (!double.TryParse(amountBox.Text.Replace("R$", "").Trim(), out amt) || amt <= 0)
                {
                    MessageBox.Show("Por favor, digite um valor válido.", "Aviso", MessageBoxButton.OK, MessageBoxImage.Warning);
                    return;
                }

                string selectedType = "APARTMENT";
                if (typeCombo.SelectedIndex == 1) selectedType = "SALARY";
                else if (typeCombo.SelectedIndex == 2) selectedType = "INVESTMENT";
                else if (typeCombo.SelectedIndex == 3) selectedType = "BOX";
                else if (typeCombo.SelectedIndex == 4) selectedType = "LENT";
                else if (typeCombo.SelectedIndex == 5) selectedType = "BILL";

                string category = (selectedType == "APARTMENT" && subcatCombo.SelectedItem != null)
                    ? subcatCombo.SelectedItem.ToString()
                    : "Geral";

                var newItem = new FinanceItem
                {
                    id = (state.items.Count > 0 ? state.items.Max(x => x.id) : 0) + 1,
                    title = string.IsNullOrWhiteSpace(titleBox.Text) ? category : titleBox.Text.Trim(),
                    amount = amt,
                    type = selectedType,
                    category = category,
                    date = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                    isCompleted = selectedType == "SALARY"
                };

                state.items.Add(newItem);
                StorageHelper.SaveState(state);
                win.Close();
                NavigateTo(currentTabIndex);
            };
            sp.Children.Add(saveBtn);

            win.Content = sp;
            win.ShowDialog();
        }

        // ==========================================
        // VIEW: CAIXINHAS & METAS
        // ==========================================
        private UIElement BuildBoxesView()
        {
            var scroll = new ScrollViewer { VerticalScrollBarVisibility = ScrollBarVisibility.Auto };
            var panel = new StackPanel { Margin = new Thickness(0, 0, 16, 16) };

            var topRow = new Grid { Margin = new Thickness(0, 0, 0, 16) };
            topRow.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            topRow.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });

            var titleSp = new StackPanel();
            titleSp.Children.Add(new TextBlock { Text = "Caixinhas & Metas", FontSize = 26, FontWeight = FontWeights.Bold, Foreground = TextWhite });
            titleSp.Children.Add(new TextBlock { Text = "Separe dinheiro para seus objetivos com metas claras", FontSize = 13, Foreground = TextMuted });
            Grid.SetColumn(titleSp, 0);

            var addBtn = new Button
            {
                Content = "➕ Nova Caixinha",
                Background = GoldAmber,
                Foreground = Brushes.Black,
                FontWeight = FontWeights.Bold,
                Padding = new Thickness(16, 10, 16, 10),
                Cursor = Cursors.Hand
            };
            addBtn.Click += delegate(object s, RoutedEventArgs e) { ShowCreateBoxModal(); };
            Grid.SetColumn(addBtn, 1);

            topRow.Children.Add(titleSp);
            topRow.Children.Add(addBtn);
            panel.Children.Add(topRow);

            var boxes = state.items.Where(i => i.type == "BOX").ToList();
            if (boxes.Count == 0)
            {
                var emptyCard = new Border
                {
                    Background = CardDark,
                    BorderBrush = BorderMuted,
                    BorderThickness = new Thickness(1),
                    CornerRadius = new CornerRadius(14),
                    Padding = new Thickness(32)
                };
                var emptySp = new StackPanel { HorizontalAlignment = HorizontalAlignment.Center };
                emptySp.Children.Add(new TextBlock { Text = "🎁 Nenhuma caixinha criada ainda.", FontSize = 16, FontWeight = FontWeights.Bold, Foreground = TextWhite, HorizontalAlignment = HorizontalAlignment.Center });
                emptySp.Children.Add(new TextBlock { Text = "Clique em 'Nova Caixinha' para começar a guardar com meta definida.", Foreground = TextMuted, Margin = new Thickness(0, 6, 0, 0) });
                emptyCard.Child = emptySp;
                panel.Children.Add(emptyCard);
            }
            else
            {
                var grid = new WrapPanel();
                foreach (var box in boxes)
                {
                    grid.Children.Add(CreateBoxCard(box));
                }
                panel.Children.Add(grid);
            }

            scroll.Content = panel;
            return scroll;
        }

        private UIElement CreateBoxCard(FinanceItem box)
        {
            var card = new Border
            {
                Background = CardDark,
                BorderBrush = BorderMuted,
                BorderThickness = new Thickness(1),
                CornerRadius = new CornerRadius(14),
                Padding = new Thickness(20),
                Width = 380,
                Margin = new Thickness(0, 0, 16, 16)
            };

            var sp = new StackPanel();
            sp.Children.Add(new TextBlock { Text = box.title, FontSize = 18, FontWeight = FontWeights.Bold, Foreground = TextWhite });
            sp.Children.Add(new TextBlock { Text = "Meta: R$ " + box.targetAmount.ToString("N2"), FontSize = 13, Foreground = GoldAmber, Margin = new Thickness(0, 2, 0, 12) });

            double pct = (box.targetAmount > 0) ? Math.Min(100.0, (box.amount / box.targetAmount) * 100.0) : 0.0;
            var progRow = new Grid { Margin = new Thickness(0, 0, 0, 4) };
            progRow.Children.Add(new TextBlock { Text = "Progresso", FontSize = 12, Foreground = TextMuted, HorizontalAlignment = HorizontalAlignment.Left });
            progRow.Children.Add(new TextBlock { Text = ((int)pct) + "%", FontSize = 12, FontWeight = FontWeights.Bold, Foreground = EmeraldGreen, HorizontalAlignment = HorizontalAlignment.Right });
            sp.Children.Add(progRow);

            var pBar = new ProgressBar { Height = 8, Value = pct, Maximum = 100, Foreground = GoldAmber, Background = CardAltDark, Margin = new Thickness(0, 0, 0, 14) };
            sp.Children.Add(pBar);

            var amtRow = new Grid { Margin = new Thickness(0, 0, 0, 14) };
            amtRow.Children.Add(new TextBlock { Text = "Saldo Guardado:", FontSize = 12, Foreground = TextMuted, HorizontalAlignment = HorizontalAlignment.Left, VerticalAlignment = VerticalAlignment.Center });
            amtRow.Children.Add(new TextBlock { Text = "R$ " + box.amount.ToString("N2"), FontSize = 18, FontWeight = FontWeights.Bold, Foreground = TextWhite, HorizontalAlignment = HorizontalAlignment.Right });
            sp.Children.Add(amtRow);

            // Action buttons
            var btnGrid = new Grid();
            btnGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            btnGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

            var resgatarBtn = new Button { Content = "Resgatar", Height = 34, Margin = new Thickness(0, 0, 4, 0), Background = CardAltDark, Foreground = TextWhite, BorderThickness = new Thickness(0), Cursor = Cursors.Hand };
            resgatarBtn.Click += delegate(object s, RoutedEventArgs e) { ShowManageBoxModal(box, false); };
            Grid.SetColumn(resgatarBtn, 0);

            var guardarBtn = new Button { Content = "Guardar", Height = 34, Margin = new Thickness(4, 0, 0, 0), Background = GoldAmber, Foreground = Brushes.Black, FontWeight = FontWeights.Bold, BorderThickness = new Thickness(0), Cursor = Cursors.Hand };
            guardarBtn.Click += delegate(object s, RoutedEventArgs e) { ShowManageBoxModal(box, true); };
            Grid.SetColumn(guardarBtn, 1);

            btnGrid.Children.Add(resgatarBtn);
            btnGrid.Children.Add(guardarBtn);
            sp.Children.Add(btnGrid);

            card.Child = sp;
            return card;
        }

        private void ShowCreateBoxModal()
        {
            var win = new Window
            {
                Title = "Nova Caixinha / Meta",
                Width = 460,
                Height = 440,
                WindowStartupLocation = WindowStartupLocation.CenterOwner,
                Owner = this,
                Background = CardDark,
                Foreground = TextWhite,
                FontFamily = new FontFamily("Segoe UI")
            };

            var sp = new StackPanel { Margin = new Thickness(24) };
            sp.Children.Add(new TextBlock { Text = "Criar Nova Caixinha", FontSize = 20, FontWeight = FontWeights.Bold, Margin = new Thickness(0, 0, 0, 16) });

            sp.Children.Add(new TextBlock { Text = "Nome da Meta (Ex: Viagem, Carro Novo):", Foreground = TextMuted, Margin = new Thickness(0, 0, 0, 4) });
            var titleBox = new TextBox { Height = 36, Margin = new Thickness(0, 0, 0, 12), Background = CardAltDark, Foreground = TextWhite, BorderBrush = BorderMuted };
            sp.Children.Add(titleBox);

            sp.Children.Add(new TextBlock { Text = "Meta Final (R$):", Foreground = TextMuted, Margin = new Thickness(0, 0, 0, 4) });
            var targetBox = new TextBox { Height = 36, Margin = new Thickness(0, 0, 0, 12), Background = CardAltDark, Foreground = TextWhite, BorderBrush = BorderMuted };
            sp.Children.Add(targetBox);

            sp.Children.Add(new TextBlock { Text = "Valor Inicial Guardado (R$):", Foreground = TextMuted, Margin = new Thickness(0, 0, 0, 4) });
            var initBox = new TextBox { Height = 36, Text = "0", Margin = new Thickness(0, 0, 0, 16), Background = CardAltDark, Foreground = TextWhite, BorderBrush = BorderMuted };
            sp.Children.Add(initBox);

            var saveBtn = new Button
            {
                Content = "Criar Caixinha",
                Background = GoldAmber,
                Foreground = Brushes.Black,
                FontWeight = FontWeights.Bold,
                Height = 40,
                Cursor = Cursors.Hand
            };
            saveBtn.Click += delegate(object s, RoutedEventArgs e)
            {
                double targetAmt, initAmt;
                double.TryParse(targetBox.Text.Trim(), out targetAmt);
                double.TryParse(initBox.Text.Trim(), out initAmt);

                if (string.IsNullOrWhiteSpace(titleBox.Text) || targetAmt <= 0)
                {
                    MessageBox.Show("Preencha o nome da meta e uma meta final válida.", "Aviso", MessageBoxButton.OK, MessageBoxImage.Warning);
                    return;
                }

                var newBox = new FinanceItem
                {
                    id = (state.items.Count > 0 ? state.items.Max(x => x.id) : 0) + 1,
                    title = titleBox.Text.Trim(),
                    amount = Math.Max(0.0, initAmt),
                    targetAmount = targetAmt,
                    type = "BOX",
                    category = "Caixinha",
                    date = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                    isCompleted = initAmt >= targetAmt
                };

                state.items.Add(newBox);
                StorageHelper.SaveState(state);
                win.Close();
                NavigateTo(2);
            };
            sp.Children.Add(saveBtn);

            win.Content = sp;
            win.ShowDialog();
        }

        private void ShowManageBoxModal(FinanceItem box, bool isDeposit)
        {
            var win = new Window
            {
                Title = isDeposit ? "Guardar na Caixinha" : "Resgatar da Caixinha",
                Width = 420,
                Height = 300,
                WindowStartupLocation = WindowStartupLocation.CenterOwner,
                Owner = this,
                Background = CardDark,
                Foreground = TextWhite,
                FontFamily = new FontFamily("Segoe UI")
            };

            var sp = new StackPanel { Margin = new Thickness(24) };
            sp.Children.Add(new TextBlock { Text = isDeposit ? "Guardar Dinheiro" : "Resgatar Dinheiro", FontSize = 18, FontWeight = FontWeights.Bold });
            sp.Children.Add(new TextBlock { Text = box.title + " (Saldo Atual: R$ " + box.amount.ToString("N2") + ")", FontSize = 13, Foreground = GoldAmber, Margin = new Thickness(0, 2, 0, 16) });

            sp.Children.Add(new TextBlock { Text = "Valor da Operação (R$):", Foreground = TextMuted, Margin = new Thickness(0, 0, 0, 4) });
            var amtBox = new TextBox { Height = 36, Margin = new Thickness(0, 0, 0, 16), Background = CardAltDark, Foreground = TextWhite, BorderBrush = BorderMuted };
            sp.Children.Add(amtBox);

            var okBtn = new Button
            {
                Content = isDeposit ? "Confirmar Depósito" : "Confirmar Resgate",
                Background = GoldAmber,
                Foreground = Brushes.Black,
                FontWeight = FontWeights.Bold,
                Height = 38,
                Cursor = Cursors.Hand
            };
            okBtn.Click += delegate(object s, RoutedEventArgs e)
            {
                double amt;
                if (!double.TryParse(amtBox.Text.Trim(), out amt) || amt <= 0)
                {
                    MessageBox.Show("Digite um valor válido.", "Aviso", MessageBoxButton.OK, MessageBoxImage.Warning);
                    return;
                }

                if (isDeposit) box.amount += amt;
                else box.amount = Math.Max(0.0, box.amount - amt);

                box.isCompleted = box.amount >= box.targetAmount;
                StorageHelper.SaveState(state);
                win.Close();
                NavigateTo(2);
            };
            sp.Children.Add(okBtn);

            win.Content = sp;
            win.ShowDialog();
        }

        // ==========================================
        // VIEW: DESAFIO 52 SEMANAS (COM SOMADOR)
        // ==========================================
        private UIElement BuildChallengeView()
        {
            var mainContainer = new Grid();

            var challengeItems = state.items.Where(i => i.type == "CHALLENGE").OrderBy(i =>
            {
                int num = 0;
                int.TryParse(i.title.Replace("Semana ", ""), out num);
                return num;
            }).ToList();

            // If empty, generate 52 weeks automatically
            if (challengeItems.Count == 0)
            {
                for (int w = 1; w <= 52; w++)
                {
                    state.items.Add(new FinanceItem
                    {
                        id = (state.items.Count > 0 ? state.items.Max(x => x.id) : 0) + 1,
                        title = "Semana " + w,
                        amount = w * state.challengeMultiplier,
                        type = "CHALLENGE",
                        category = "MULTIPLIER_" + ((int)state.challengeMultiplier),
                        isCompleted = false,
                        date = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
                    });
                }
                StorageHelper.SaveState(state);
                challengeItems = state.items.Where(i => i.type == "CHALLENGE").ToList();
            }

            var scroll = new ScrollViewer { VerticalScrollBarVisibility = ScrollBarVisibility.Auto };
            var panel = new StackPanel { Margin = new Thickness(0, 0, 16, 110) };

            // Header
            panel.Children.Add(new TextBlock { Text = "Desafio das 52 Semanas", FontSize = 26, FontWeight = FontWeights.Bold, Foreground = TextWhite });
            panel.Children.Add(new TextBlock { Text = "Clique nas semanas para calcular a soma de depósito em tempo real", FontSize = 13, Foreground = TextMuted, Margin = new Thickness(0, 2, 0, 16) });

            // Progress Summary
            int completed = challengeItems.Count(i => i.isCompleted);
            double totalSaved = challengeItems.Where(i => i.isCompleted).Sum(i => i.amount);
            double totalGoal = challengeItems.Sum(i => i.amount);

            var summaryCard = new Border
            {
                Background = CardDark,
                BorderBrush = BorderMuted,
                BorderThickness = new Thickness(1),
                CornerRadius = new CornerRadius(14),
                Padding = new Thickness(18),
                Margin = new Thickness(0, 0, 0, 16)
            };
            var sGrid = new Grid();
            sGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            sGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            sGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

            sGrid.Children.Add(CreateStatBlock("PROGRESSO", completed + " de 52 semanas (" + (completed * 100 / 52) + "%)", EmeraldGreen, 0));
            sGrid.Children.Add(CreateStatBlock("TOTAL GUARDADO", "R$ " + totalSaved.ToString("N2"), EmeraldGreen, 1));
            sGrid.Children.Add(CreateStatBlock("META FINAL ANUAL", "R$ " + totalGoal.ToString("N2"), TextWhite, 2));

            summaryCard.Child = sGrid;
            panel.Children.Add(summaryCard);

            // 52 Week Grid (4 columns x 13 rows)
            var weekGrid = new UniformGrid { Columns = 4 };
            foreach (var wItem in challengeItems)
            {
                weekGrid.Children.Add(CreateChallengeWeekCard(wItem));
            }
            panel.Children.Add(weekGrid);

            scroll.Content = panel;
            mainContainer.Children.Add(scroll);

            // Floating Popup for Single / Multi-selection sum
            floatingChallengePopup = new Border
            {
                Background = CardDark,
                BorderBrush = EmeraldGreen,
                BorderThickness = new Thickness(2),
                CornerRadius = new CornerRadius(16),
                Padding = new Thickness(20, 14, 20, 14),
                VerticalAlignment = VerticalAlignment.Bottom,
                Margin = new Thickness(20, 0, 20, 20),
                Visibility = Visibility.Collapsed
            };
            mainContainer.Children.Add(floatingChallengePopup);

            UpdateFloatingChallengePopup();

            return mainContainer;
        }

        private UIElement CreateChallengeWeekCard(FinanceItem weekItem)
        {
            bool isSelected = selectedChallengeWeekIds.Contains(weekItem.id);
            var card = new Border
            {
                Background = isSelected ? new SolidColorBrush(Color.FromArgb(50, 0, 230, 118))
                                        : (weekItem.isCompleted ? new SolidColorBrush(Color.FromArgb(25, 0, 230, 118)) : CardAltDark),
                BorderBrush = isSelected ? EmeraldGreen : (weekItem.isCompleted ? EmeraldGreen : BorderMuted),
                BorderThickness = new Thickness(isSelected ? 2 : 1),
                CornerRadius = new CornerRadius(10),
                Padding = new Thickness(12),
                Margin = new Thickness(4),
                Cursor = Cursors.Hand
            };

            var sp = new StackPanel();
            var topRow = new Grid();
            topRow.Children.Add(new TextBlock
            {
                Text = weekItem.title,
                FontWeight = isSelected ? FontWeights.ExtraBold : FontWeights.Bold,
                Foreground = isSelected ? EmeraldGreen : TextWhite,
                HorizontalAlignment = HorizontalAlignment.Left
            });
            if (weekItem.isCompleted)
            {
                topRow.Children.Add(new TextBlock
                {
                    Text = "✓",
                    FontWeight = FontWeights.Bold,
                    Foreground = EmeraldGreen,
                    HorizontalAlignment = HorizontalAlignment.Right
                });
            }
            sp.Children.Add(topRow);

            sp.Children.Add(new TextBlock
            {
                Text = "R$ " + weekItem.amount.ToString("N2"),
                FontSize = 13,
                FontWeight = FontWeights.SemiBold,
                Foreground = (isSelected || weekItem.isCompleted) ? EmeraldGreen : TextMuted,
                Margin = new Thickness(0, 4, 0, 0)
            });

            card.Child = sp;

            card.MouseDown += delegate(object s, MouseButtonEventArgs e)
            {
                if (selectedChallengeWeekIds.Contains(weekItem.id))
                    selectedChallengeWeekIds.Remove(weekItem.id);
                else
                    selectedChallengeWeekIds.Add(weekItem.id);

                NavigateTo(3);
            };

            return card;
        }

        private void UpdateFloatingChallengePopup()
        {
            if (floatingChallengePopup == null) return;
            if (selectedChallengeWeekIds.Count == 0)
            {
                floatingChallengePopup.Visibility = Visibility.Collapsed;
                return;
            }

            floatingChallengePopup.Visibility = Visibility.Visible;
            var selectedItems = state.items.Where(i => selectedChallengeWeekIds.Contains(i.id)).ToList();
            double sum = selectedItems.Sum(i => i.amount);
            bool allDone = selectedItems.All(i => i.isCompleted);

            var grid = new Grid();
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(2, GridUnitType.Star) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1.5, GridUnitType.Star) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1.5, GridUnitType.Star) });

            var infoSp = new StackPanel();
            infoSp.Children.Add(new TextBlock
            {
                Text = selectedItems.Count == 1 ? selectedItems[0].title : (selectedItems.Count + " Semanas Selecionadas"),
                FontSize = 16,
                FontWeight = FontWeights.Bold,
                Foreground = TextWhite
            });
            infoSp.Children.Add(new TextBlock
            {
                Text = selectedItems.Count == 1 ? "Valor a ser depositado" : "Soma combinada das semanas",
                FontSize = 12,
                Foreground = TextMuted
            });
            Grid.SetColumn(infoSp, 0);

            var amtSp = new StackPanel { HorizontalAlignment = HorizontalAlignment.Center };
            amtSp.Children.Add(new TextBlock { Text = "SOMA TOTAL:", FontSize = 10, FontWeight = FontWeights.SemiBold, Foreground = TextMuted });
            amtSp.Children.Add(new TextBlock { Text = "R$ " + sum.ToString("N2"), FontSize = 20, FontWeight = FontWeights.Bold, Foreground = EmeraldGreen });
            Grid.SetColumn(amtSp, 1);

            var btnRow = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right };
            var clearBtn = new Button
            {
                Content = "Limpar",
                Padding = new Thickness(12, 6, 12, 6),
                Background = CardAltDark,
                Foreground = TextWhite,
                BorderThickness = new Thickness(0),
                Margin = new Thickness(0, 0, 8, 0),
                Cursor = Cursors.Hand
            };
            clearBtn.Click += delegate(object s, RoutedEventArgs e)
            {
                selectedChallengeWeekIds.Clear();
                NavigateTo(3);
            };

            var confirmBtn = new Button
            {
                Content = allDone ? "Desmarcar" : "Confirmar Depósito",
                Padding = new Thickness(16, 6, 16, 6),
                Background = allDone ? GoldAmber : EmeraldGreen,
                Foreground = Brushes.Black,
                FontWeight = FontWeights.Bold,
                BorderThickness = new Thickness(0),
                Cursor = Cursors.Hand
            };
            confirmBtn.Click += delegate(object s, RoutedEventArgs e)
            {
                bool target = !allDone;
                foreach (var item in selectedItems) item.isCompleted = target;
                StorageHelper.SaveState(state);
                selectedChallengeWeekIds.Clear();
                NavigateTo(3);
            };

            btnRow.Children.Add(clearBtn);
            btnRow.Children.Add(confirmBtn);
            Grid.SetColumn(btnRow, 2);

            grid.Children.Add(infoSp);
            grid.Children.Add(amtSp);
            grid.Children.Add(btnRow);

            floatingChallengePopup.Child = grid;
        }

        // ==========================================
        // VIEW: EMPRÉSTIMOS
        // ==========================================
        private UIElement BuildLentView()
        {
            var scroll = new ScrollViewer { VerticalScrollBarVisibility = ScrollBarVisibility.Auto };
            var panel = new StackPanel { Margin = new Thickness(0, 0, 16, 16) };

            panel.Children.Add(new TextBlock { Text = "Empréstimos Ativos", FontSize = 26, FontWeight = FontWeights.Bold, Foreground = TextWhite });
            panel.Children.Add(new TextBlock { Text = "Monitore valores emprestados e controle de pagamentos", FontSize = 13, Foreground = TextMuted, Margin = new Thickness(0, 2, 0, 16) });

            var lentItems = state.items.Where(i => i.type == "LENT").ToList();
            if (lentItems.Count == 0)
            {
                var emptyCard = new Border
                {
                    Background = CardDark,
                    BorderBrush = BorderMuted,
                    BorderThickness = new Thickness(1),
                    CornerRadius = new CornerRadius(14),
                    Padding = new Thickness(32)
                };
                emptyCard.Child = new TextBlock
                {
                    Text = "🤝 Nenhum valor emprestado no momento!",
                    FontSize = 15,
                    Foreground = TextMuted,
                    HorizontalAlignment = HorizontalAlignment.Center
                };
                panel.Children.Add(emptyCard);
            }
            else
            {
                var card = new Border
                {
                    Background = CardDark,
                    BorderBrush = BorderMuted,
                    BorderThickness = new Thickness(1),
                    CornerRadius = new CornerRadius(14),
                    Padding = new Thickness(16)
                };
                var sp = new StackPanel();
                foreach (var item in lentItems)
                {
                    sp.Children.Add(CreateTransactionRow(item));
                }
                card.Child = sp;
                panel.Children.Add(card);
            }

            scroll.Content = panel;
            return scroll;
        }

        // ==========================================
        // VIEW: SINCRONIZAÇÃO ANDROID (WI-FI P2P)
        // ==========================================
        private UIElement BuildSyncView()
        {
            var scroll = new ScrollViewer { VerticalScrollBarVisibility = ScrollBarVisibility.Auto };
            var panel = new StackPanel { Margin = new Thickness(0, 0, 16, 16), MaxWidth = 600, HorizontalAlignment = HorizontalAlignment.Left };

            panel.Children.Add(new TextBlock { Text = "Sincronização com Celular Android", FontSize = 26, FontWeight = FontWeights.Bold, Foreground = TextWhite });
            panel.Children.Add(new TextBlock { Text = "Transfira seus dados pela rede Wi-Fi local com segurança total e sem nuvem pública", FontSize = 13, Foreground = TextMuted, Margin = new Thickness(0, 2, 0, 20) });

            var card = new Border
            {
                Background = CardDark,
                BorderBrush = BorderMuted,
                BorderThickness = new Thickness(1),
                CornerRadius = new CornerRadius(16),
                Padding = new Thickness(24)
            };

            var sp = new StackPanel { Margin = new Thickness(0) };

            sp.Children.Add(new TextBlock { Text = "Passo 1: Abra a Sincronização no seu celular", FontSize = 14, FontWeight = FontWeights.Bold, Foreground = OceanBlue, Margin = new Thickness(0, 0, 0, 4) });
            sp.Children.Add(new TextBlock { Text = "No aplicativo Android, toque no ícone de sincronização (topo da tela) para gerar o IP e o PIN de 4 dígitos.", FontSize = 12, Foreground = TextMuted, Margin = new Thickness(0, 0, 0, 16) });

            sp.Children.Add(new TextBlock { Text = "Endereço IP do Celular (ex: 192.168.1.50):", Foreground = TextWhite, Margin = new Thickness(0, 0, 0, 4) });
            var ipBox = new TextBox { Height = 38, Text = "192.168.1.", Background = CardAltDark, Foreground = TextWhite, BorderBrush = BorderMuted, Margin = new Thickness(0, 0, 0, 12), Padding = new Thickness(8, 6, 8, 6) };
            sp.Children.Add(ipBox);

            sp.Children.Add(new TextBlock { Text = "PIN de 4 dígitos exibido no celular:", Foreground = TextWhite, Margin = new Thickness(0, 0, 0, 4) });
            var pinBox = new TextBox { Height = 38, MaxLength = 4, Background = CardAltDark, Foreground = EmeraldGreen, FontSize = 16, FontWeight = FontWeights.Bold, BorderBrush = BorderMuted, Margin = new Thickness(0, 0, 0, 20), Padding = new Thickness(8, 6, 8, 6) };
            sp.Children.Add(pinBox);

            var statusBlock = new TextBlock
            {
                Text = "Pronto para sincronizar.",
                FontSize = 13,
                Foreground = TextMuted,
                Margin = new Thickness(0, 0, 0, 16),
                TextWrapping = TextWrapping.Wrap
            };

            var syncBtn = new Button
            {
                Content = "🔄 Sincronizar Agora com Android",
                Background = OceanBlue,
                Foreground = Brushes.Black,
                FontWeight = FontWeights.Bold,
                Height = 44,
                Cursor = Cursors.Hand
            };

            syncBtn.Click += async delegate(object s, RoutedEventArgs e)
            {
                string ip = ipBox.Text.Trim();
                string pin = pinBox.Text.Trim();

                if (string.IsNullOrWhiteSpace(ip) || pin.Length != 4)
                {
                    MessageBox.Show("Preencha o IP e o PIN de 4 dígitos.", "Aviso", MessageBoxButton.OK, MessageBoxImage.Warning);
                    return;
                }

                syncBtn.IsEnabled = false;
                statusBlock.Text = "Conectando ao celular em " + ip + ":8765...";
                statusBlock.Foreground = OceanBlue;

                try
                {
                    var payload = new SyncPayload
                    {
                        deviceName = Environment.MachineName + " (Windows)",
                        timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                        items = state.items,
                        apartmentSubcategories = state.apartmentSubcategories
                    };

                    string jsonPayload = SimpleJson.Serialize(payload);

                    using (var client = new HttpClient())
                    {
                        client.Timeout = TimeSpan.FromSeconds(15);
                        var request = new HttpRequestMessage(HttpMethod.Post, "http://" + ip + ":8765/sync");
                        request.Headers.Add("X-Sync-Pin", pin);
                        request.Content = new StringContent(jsonPayload, Encoding.UTF8, "application/json");

                        var response = await client.SendAsync(request);
                        string responseBody = await response.Content.ReadAsStringAsync();

                        if (response.IsSuccessStatusCode)
                        {
                            var syncResp = SimpleJson.Deserialize<SyncResponse>(responseBody);
                            if (syncResp != null && syncResp.success)
                            {
                                // Merge server items into state
                                state.items = syncResp.serverItems;
                                if (syncResp.serverApartmentSubcategories != null && syncResp.serverApartmentSubcategories.Count > 0)
                                {
                                    state.apartmentSubcategories = syncResp.serverApartmentSubcategories;
                                }

                                StorageHelper.SaveState(state);
                                statusBlock.Text = "✓ Sincronização concluída com sucesso! " + state.items.Count + " registros atualizados.";
                                statusBlock.Foreground = EmeraldGreen;
                                MessageBox.Show("Sincronização com Android concluída com sucesso!", "Sucesso", MessageBoxButton.OK, MessageBoxImage.Information);
                            }
                            else
                            {
                                statusBlock.Text = "Falha: " + (syncResp != null ? syncResp.message : "Resposta inválida.");
                                statusBlock.Foreground = CoralRed;
                            }
                        }
                        else
                        {
                            statusBlock.Text = "Erro (" + response.StatusCode + "): Verifique o PIN digitado.";
                            statusBlock.Foreground = CoralRed;
                        }
                    }
                }
                catch (Exception ex)
                {
                    statusBlock.Text = "Erro de conexão: " + ex.Message + " (Certifique-se de que o celular está no mesmo Wi-Fi e a tela de sincronização aberta).";
                    statusBlock.Foreground = CoralRed;
                }
                finally
                {
                    syncBtn.IsEnabled = true;
                }
            };

            sp.Children.Add(statusBlock);
            sp.Children.Add(syncBtn);
            card.Child = sp;
            panel.Children.Add(card);

            scroll.Content = panel;
            return scroll;
        }
    }

    // ==========================================
    // ENTRY POINT
    // ==========================================
    public static class Program
    {
        [STAThread]
        public static void Main()
        {
            var app = new Application();
            app.Run(new MainWindow());
        }
    }
}
