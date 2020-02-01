object Errors
  final case class MissingArgError()
  final case class FileNotFoundError()
  final case class ArithmeticError()
  final case class NumberFormatError()
  final case class AccessControlError()

  type ProgramError =
    MissingArgError | 
    AccessControlError | 
    FileNotFoundError | 
    ArithmeticError | 
    NumberFormatError