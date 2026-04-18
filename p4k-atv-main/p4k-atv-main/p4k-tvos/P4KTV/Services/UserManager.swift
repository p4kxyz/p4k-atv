import Foundation

struct User: Codable {
    let userId: String
    let name: String
    let email: String
    let status: String? // "active" or "inactive"
    
    enum CodingKeys: String, CodingKey {
        case userId = "user_id"
        case name
        case email
        case status
    }
}

class UserManager: ObservableObject {
    static let shared = UserManager()
    
    @Published var currentUser: User?
    @Published var isLoggedIn: Bool = false
    
    private init() {
        loadUser()
    }
    
    func saveUser(_ user: User) {
        self.currentUser = user
        self.isLoggedIn = true
        if let data = try? JSONEncoder().encode(user) {
            UserDefaults.standard.set(data, forKey: "saved_user")
        }
    }
    
    func loadUser() {
        if let data = UserDefaults.standard.data(forKey: "saved_user"),
           let user = try? JSONDecoder().decode(User.self, from: data) {
            self.currentUser = user
            self.isLoggedIn = true
        }
    }
    
    func logout() {
        self.currentUser = nil
        self.isLoggedIn = false
        UserDefaults.standard.removeObject(forKey: "saved_user")
    }
}
