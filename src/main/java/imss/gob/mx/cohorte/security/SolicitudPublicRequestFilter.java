package imss.gob.mx.cohorte.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import imss.gob.mx.cohorte.services.solicitud.security.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.*;
import java.time.Duration;
import java.util.Map;

@Component @Order(-90)
public class SolicitudPublicRequestFilter extends OncePerRequestFilter {
    private final SolicitudThrottle throttle;
    private final SolicitudClientIp ips;
    private final ObjectMapper mapper;
    public SolicitudPublicRequestFilter(SolicitudThrottle throttle,SolicitudClientIp ips,ObjectMapper mapper){this.throttle=throttle;this.ips=ips;this.mapper=mapper;}
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/publico/solicitud-datos") || !"POST".equals(request.getMethod());
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        try {
            throttle.acquire("request-ip:"+ips.resolve(request),60,Duration.ofHours(1));
            if (request.getContentType()==null || !request.getContentType().split(";")[0].strip().equalsIgnoreCase("application/json")) { error(response,415,"Se requiere JSON."); return; }
            if (request.getContentLengthLong()>16384) {error(response,413,"La solicitud es demasiado grande.");return;}
            byte[] body=request.getInputStream().readNBytes(16385);
            if(body.length>16384){error(response,413,"La solicitud es demasiado grande.");return;}
            HttpServletRequestWrapper wrapper=new HttpServletRequestWrapper(request){
                @Override public ServletInputStream getInputStream(){
                    ByteArrayInputStream source=new ByteArrayInputStream(body);
                    return new ServletInputStream(){
                        @Override public int read(){return source.read();}
                        @Override public int read(byte[] b,int off,int len){return source.read(b,off,len);}
                        @Override public boolean isFinished(){return source.available()==0;}
                        @Override public boolean isReady(){return true;}
                        @Override public void setReadListener(ReadListener listener){throw new UnsupportedOperationException();}
                    };
                }
                @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),java.nio.charset.StandardCharsets.UTF_8));}
            };
            chain.doFilter(wrapper,response);
        } catch(SolicitudSecurityException e){error(response,e.getStatus().value(),e.getMessage());}
    }
    private void error(HttpServletResponse response,int status,String message)throws IOException{
        response.setStatus(status);response.setContentType("application/json");response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(),Map.of("error",true,"message",message));
    }
}
