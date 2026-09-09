package cfbd.co.sgt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import cfbd.co.sgt.model.Usuario;

@Repository 
public interface UsuarioRepository extends JpaRepository<Usuario, String> {
    // Buscar usuario por correo electrónico
    Optional<Usuario> findByEmail(String email);

}
