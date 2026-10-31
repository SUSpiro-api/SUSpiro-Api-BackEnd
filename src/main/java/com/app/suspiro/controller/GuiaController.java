package com.app.suspiro.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.suspiro.dto.GuiaAgendamentoRequest;
import com.app.suspiro.dto.GuiaCadastroRequest;
import com.app.suspiro.dto.GuiaResponse;
import com.app.suspiro.model.GuiaModel;
import com.app.suspiro.model.UsuariosModel;
import com.app.suspiro.enums.Perfil;
import com.app.suspiro.security.SecurityAuth;
import com.app.suspiro.service.GuiaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/guias")
public class GuiaController {

    private final GuiaService guiaService;

    public GuiaController(GuiaService guiaService) {
        this.guiaService = guiaService;
    }

    @Operation(
            summary = "Cadastra uma guia",
            description = "Permite emissão por Super Admin, médico e enfermeiro; valida o paciente e usa os dados oficiais do cadastro dele. "
                    + "pacienteNome e pacienteCpf enviados no corpo são ignorados.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Guia cadastrada com status AGUARDANDO_AGENDAMENTO."),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado.", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MEDICO', 'ENFERMEIRO')")
    public ResponseEntity<GuiaResponse> cadastrar(
            @Valid @RequestBody GuiaCadastroRequest request,
            Authentication authentication) {

        GuiaModel guia = guiaService.cadastrar(request, usuarioLogado(authentication));

        return ResponseEntity
                .created(URI.create("/guias/" + guia.getId()))
                .body(GuiaResponse.de(guia));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GERENTE', 'ATENDENTE', 'ENFERMEIRO')")
    public List<GuiaResponse> listarTodas() {
        return guiaService.listarTodas().stream().map(GuiaResponse::de).toList();
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'ENFERMEIRO', 'ATENDENTE', 'GERENTE', 'SUPER_ADMIN')")
    public List<GuiaResponse> listarMinhas(Authentication authentication) {
        UsuariosModel usuario = usuarioLogado(authentication);
        List<GuiaModel> guias;
        if (usuario.getPerfil() == Perfil.PACIENTE) {
            if (usuario.getPacienteId() == null) {
                throw new com.app.suspiro.exception.RecursoNaoEncontradoException(
                        "A conta do paciente não possui vínculo com seu cadastro. Procure a recepção.");
            }
            guias = guiaService.listarDoPaciente(usuario.getPacienteId());
        } else {
            guias = guiaService.listarDoSolicitante(usuario);
        }
        boolean incluirCpfCompleto = usuario.getPerfil() != Perfil.PACIENTE;
        return guias.stream().map(guia -> GuiaResponse.de(guia, incluirCpfCompleto)).toList();
    }

    @GetMapping("/pendentes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GERENTE', 'ATENDENTE', 'ENFERMEIRO', 'MEDICO')")
    public List<GuiaResponse> listarPendentes(Authentication authentication) {
        boolean incluirCpfCompleto = usuarioLogado(authentication).getPerfil() != Perfil.MEDICO;
        return guiaService.listarPendentes()
                .stream()
                .map(guia -> GuiaResponse.de(guia, incluirCpfCompleto))
                .toList();
    }

    @Operation(
            summary = "Histórico de guias do paciente",
            description = "Retorna todas as guias do paciente, de qualquer profissional solicitante, "
                    + "da mais recente para a mais antiga (empate: maior id primeiro). "
                    + "Requer autenticação e perfil SUPER_ADMIN, GERENTE, MEDICO ou ENFERMEIRO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta realizada; lista vazia se o paciente não tiver guias.",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GuiaResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão para consultar o histórico.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado.", content = @Content)
    })
    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GERENTE', 'MEDICO', 'ENFERMEIRO')")
    public List<GuiaResponse> listarDoPaciente(
            @Parameter(description = "Id (UUID) do paciente cadastrado.", required = true,
                    example = "3f2b8c1e-7a4d-4e9b-9c1a-2d5e6f7a8b9c")
            @PathVariable UUID pacienteId) {

        return guiaService.listarDoPaciente(pacienteId)
                .stream()
                .map(GuiaResponse::de)
                .toList();
    }

    @GetMapping("/protocolo/{protocolo}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GERENTE', 'ATENDENTE', 'MEDICO', 'ENFERMEIRO')")
    public GuiaResponse buscarPorProtocolo(@PathVariable String protocolo) {
        return GuiaResponse.de(guiaService.buscarPorProtocolo(protocolo));
    }

    @PatchMapping("/{id}/agendamento")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GERENTE', 'ATENDENTE', 'ENFERMEIRO')")
    public GuiaResponse agendar(
            @PathVariable Long id,
            @Valid @RequestBody GuiaAgendamentoRequest request,
            Authentication authentication) {

        return GuiaResponse.de(guiaService.agendar(id, request, usuarioLogado(authentication)));
    }

    private UsuariosModel usuarioLogado(Authentication authentication) {

        return ((SecurityAuth) authentication.getPrincipal()).getUsuario();
    }
}
