import Foundation
import Combine

class NetworkManager {
    static let shared = NetworkManager()
    
    private init() {}
    
    func request<T: Decodable>(endpoint: String, method: String = "GET", parameters: [String: Any]? = nil, completion: @escaping (Result<T, Error>) -> Void) {
        guard let url = URL(string: "\(Config.baseUrl)\(endpoint)") else {
            completion(.failure(NSError(domain: "Invalid URL", code: 0, userInfo: nil)))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.addValue(Config.apiKey, forHTTPHeaderField: "API-KEY")
        
        if let parameters = parameters, method == "POST" {
            // Form URL Encoded for PHP API usually
            var components = URLComponents()
            components.queryItems = parameters.map { URLQueryItem(name: $0.key, value: "\($0.value)") }
            request.httpBody = components.query?.data(using: .utf8)
            request.addValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        } else if let parameters = parameters, method == "GET" {
             // Append to URL for GET
             var components = URLComponents(url: url, resolvingAgainstBaseURL: true)
             components?.queryItems = parameters.map { URLQueryItem(name: $0.key, value: "\($0.value)") }
             request.url = components?.url
        }
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(error))
                return
            }
            
            guard let data = data else {
                completion(.failure(NSError(domain: "No Data", code: 0, userInfo: nil)))
                return
            }
            
            // Debug: Print JSON String
            // if let jsonStr = String(data: data, encoding: .utf8) { print("Response: \(jsonStr)") }
            
            do {
                let decoder = JSONDecoder()
                let result = try decoder.decode(T.self, from: data)
                completion(.success(result))
            } catch {
                print("Decoding error for \(endpoint): \(error)")
                completion(.failure(error))
            }
        }.resume()
    }

    func fetchHomeContent(completion: @escaping (Result<HomeResponse, Error>) -> Void) {
        request(endpoint: "home_content_for_android", completion: completion)
    }
    
    func fetchMovieDetails(id: String, type: String, completion: @escaping (Result<MovieDetails, Error>) -> Void) {
        let params = ["id": id, "type": type]
        request(endpoint: "single_details", parameters: params, completion: completion)
    }
}
