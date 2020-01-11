package controllers

import domain.Candidate.{Java, Spark}
import domain.JdkDistro._
import domain.Platform._
import domain.{Candidate, Platform}
import javax.inject.Inject
import play.api.Logging
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HooksController @Inject() (cc: ControllerComponents)
    extends AbstractController(cc)
    with Logging {

  def hook(
      phase: String,
      candidateId: String,
      version: String,
      platformId: String
  ): Action[AnyContent] =
    Action.async { _ =>
      Future {
        implicit val candidate = Candidate(candidateId)

        val platform = Platform(platformId).getOrElse(Universal)

        logger.info(s"$phase install hook requested for: $candidateId $version ${platform.name}")

        (phase, candidate, normalise(version), platform, vendor(version)) match {

          //POST: Mac OSX
          case (Post.phase, Java, _, MacOSX, BellSoft) =>
            Ok(views.txt.default_post_zip(candidate, version, MacOSX))
          case (Post.phase, Java, _, MacOSX, Zulu) =>
            Ok(views.txt.default_post_tarball(candidate, version, MacOSX))
          case (Post.phase, Java, _, MacOSX, ZuluFX) =>
            Ok(views.txt.default_post_tarball(candidate, version, MacOSX))
          case (Post.phase, Java, _, MacOSX, _) =>
            Ok(views.txt.java_post_openjdk_osx(candidate, version, MacOSX))

          //POST: Linux
          case (Post.phase, Java, _, Linux, _) =>
            Ok(views.txt.java_post_linux_tarball(candidate, version, Linux))

          //POST: Cygwin
          case (Post.phase, Java, _, Windows64Cygwin, Oracle) =>
            Ok(views.txt.java_post_cygwin_msi(candidate, version, Windows64Cygwin))
          case (Post.phase, Java, "9", Windows64Cygwin, OpenJDK) =>
            Ok(views.txt.default_post_tarball(candidate, version, Windows64Cygwin))
          case (Post.phase, Java, "10", Windows64Cygwin, OpenJDK) =>
            Ok(views.txt.default_post_tarball(candidate, version, Windows64Cygwin))
          case (Post.phase, Java, _, Windows64Cygwin, _) =>
            Ok(views.txt.default_post_zip(candidate, version, Windows64Cygwin))

          //POST: Mysys
          case (Post.phase, Java, "9", Windows64MinGW, OpenJDK) =>
            Ok(views.txt.default_post_tarball(candidate, version, Windows64MinGW))
          case (Post.phase, Java, "10", Windows64MinGW, OpenJDK) =>
            Ok(views.txt.default_post_tarball(candidate, version, Windows64MinGW))
          case (Post.phase, Java, _, Windows64MinGW, _) =>
            Ok(views.txt.default_post_zip(candidate, version, Windows64MinGW))

          //POST
          case (Post.phase, Java, _, _, _) =>
            NotFound
          case (Post.phase, Spark, _, _, _) =>
            Ok(views.txt.default_post_tarball(candidate, version, platform))
          case (Post.phase, _, _, _, _) =>
            Ok(views.txt.default_post_zip(candidate, version, platform))

          //PRE
          case (Pre.phase, Java, _, Windows64MinGW, Oracle) =>
            Ok(views.txt.java_pre_mingw_msi(candidate, version, Windows64MinGW))
          case (Pre.phase, _, _, _, _) =>
            Ok(views.txt.default_pre(candidate, version, platform))

          case (_, _, _, _, _) =>
            NotFound
        }
      }
    }

  private def normalise(version: String)(implicit c: Candidate): String =
    if (c == Java) version.split('.').head else version

  private def dropSuffix(v: String) = v.split("-").head

  private def vendor(version: String) = version.split("-").lastOption.getOrElse("")
}

sealed trait Hooks {
  val phase: String
}
case object Post extends Hooks {
  override val phase = "post"
}
case object Pre extends Hooks {
  override val phase = "pre"
}
