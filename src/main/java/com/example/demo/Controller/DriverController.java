package com.example.demo.Controller;

import com.example.demo.Services.DriverService;
import com.example.demo.dtos.DriverDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
@CrossOrigin(origins = "*")
public class DriverController {

    @Autowired private DriverService driverService;

    // POST /api/drivers/register
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody DriverDto.RegisterRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(driverService.register(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/drivers/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody DriverDto.LoginRequest req) {
        try {
        	return ResponseEntity.ok(driverService.login(req.getUsername(), req.getPassword(), req.getFcmToken()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/drivers/{driverId}/upload-photo
    // Driver registration ke baad photo upload karo
    @PostMapping("/{driverId}/upload-photo")
    public ResponseEntity<?> uploadPhoto(
            @PathVariable String driverId,
            @RequestParam("photo") MultipartFile photo) {
        try {
            if (photo.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Photo file empty hai"));
            }
            String photoUrl = driverService.saveDriverPhoto(driverId, photo);
            return ResponseEntity.ok(Map.of(
                "message",  "Photo upload ho gayi!",
                "driverId", driverId,
                "photoUrl", photoUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/drivers/{driverId}/photo
    // Driver ki photo serve karo — frontend image tag mein directly use kar sakta hai
    @GetMapping("/{driverId}/photo")
    public ResponseEntity<Resource> getPhoto(@PathVariable String driverId) {
        try {
            Path photoPath = driverService.getDriverPhotoPath(driverId);
            if (photoPath == null) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(photoPath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // File extension se content type detect karo
            String filename = photoPath.getFileName().toString().toLowerCase();
            MediaType mediaType = MediaType.IMAGE_JPEG;
            if (filename.endsWith(".png"))  mediaType = MediaType.IMAGE_PNG;
            if (filename.endsWith(".webp")) mediaType = MediaType.parseMediaType("image/webp");

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=86400") // 1 din cache
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    
    
//    @PostMapping("/save-token")
//    public ResponseEntity<?> saveDriverToken(@RequestBody Map<String, String> body) {
//
//        String driverId = body.get("driverId");
//
//        String token = body.get("token");
//        
//        
//        
//        System.out.println("DriverId: " + driverId);
//        System.out.println("Token##################################################: " + token);
//
//        driverService.saveDriverToken(driverId, token);
//
//        return ResponseEntity.ok("Token saved");
//    }
//    
    
    
    @PostMapping("/save-token")
    public ResponseEntity<?> saveDriverToken(@RequestBody Map<String, String> body) {

        System.out.println("🔥 API HIT HO GAYI");

        return ResponseEntity.ok("OK");
    }
}